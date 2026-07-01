# YAS Service Mesh Runbook

Thư mục này chứa manifest Istio cho phần TV4: mTLS, retry policy, AuthorizationPolicy, test plan và hướng dẫn kiểm tra thủ công.

## Phạm vi

- Namespace chính để test: `dev`.
- `staging` có manifest mTLS cơ bản, chỉ apply sau khi `dev` ổn định.
- Không apply AuthorizationPolicy cho `developer-build` trong giai đoạn này.
- Không apply `deny-all` cho đến khi các bước mTLS, retry, Kiali và test baseline đã hoàn thành.

## Cấu trúc

```text
istio/
├── mtls/
├── virtual-services/
├── authorization/
│   ├── allow/
│   └── deny-final/
└── tests/
```

Lưu ý quan trọng: trong Istio, chỉ cần apply một `AuthorizationPolicy` dạng `ALLOW` cho workload là workload đó bắt đầu deny các request không match rule. Vì vậy không apply toàn bộ `istio/authorization/` sớm.

## Kiểm tra trước khi apply

```bash
kubectl get pods -n istio-system
kubectl get ns dev --show-labels
kubectl get pods -n dev
kubectl get deploy -n dev -o jsonpath='{range .items[*]}{.metadata.name}{" -> sa="}{.spec.template.spec.serviceAccountName}{" labels="}{.spec.template.metadata.labels}{"\n"}{end}'
```

Điều kiện tối thiểu:

- `istiod`, `kiali`, `prometheus` Running.
- Namespace `dev` có label `istio-injection=enabled`.
- App pods trong `dev` đạt `2/2 Running`.
- ServiceAccount khớp service name, ví dụ `product -> sa=product`.
- Pod labels có `app=<service>`.

## Thứ tự apply an toàn

### 1. Apply mTLS

```bash
kubectl apply -f istio/mtls/peer-authentication-dev.yaml
kubectl apply -f istio/mtls/destination-rule-dev.yaml
```

Verify:

```bash
POD=$(kubectl get pods -n dev -l app=product -o jsonpath='{.items[0].metadata.name}')
istioctl x describe pod "$POD" -n dev
```

Kỳ vọng: output thể hiện workload đang dùng mTLS, hoặc trong Kiali graph có biểu tượng lock.

### 2. Apply retry VirtualService

```bash
kubectl apply -f istio/virtual-services/
```

Verify:

```bash
kubectl get virtualservice -n dev
kubectl describe virtualservice product-retry -n dev
```

### 3. Kiểm tra Kiali

```bash
kubectl get svc kiali -n istio-system
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

Trong Kiali:

- Graph -> Namespace `dev`.
- Display -> Security để thấy mTLS lock.
- Display -> Traffic Animation nếu cần.
- Workloads/Services -> chọn `product`, `order`, `cart` để xem metrics.

### 4. Generate traffic thủ công

Chạy từ một pod có sidecar trong namespace `dev`:

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')

kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Nếu cần tạo traffic liên tục cho Kiali:

```bash
while true; do
  kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
    curl -s http://product.dev.svc.cluster.local:8080/product/actuator/health >/dev/null
  sleep 2
done
```

### 5. AuthorizationPolicy chỉ apply ở cuối

Khi team đã sẵn sàng test chặn/mở kết nối:

```bash
kubectl apply -f istio/authorization/allow/
kubectl apply -f istio/authorization/deny-final/dev-deny-all.yaml
```

Test allowed:

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Test denied:

```bash
SEARCH=$(kubectl get pods -n dev -l app=search -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$SEARCH" -c search -- \
  curl -v http://payment.dev.svc.cluster.local:8081/payment/actuator/health
```

Kỳ vọng denied test trả `403` hoặc `RBAC: access denied`.

Rollback AuthorizationPolicy:

```bash
kubectl delete authorizationpolicy --all -n dev
```

## Dry-run trước khi apply

Nếu Istio CRDs đã cài:

```bash
kubectl apply --dry-run=server -f istio/mtls/
kubectl apply --dry-run=server -f istio/virtual-services/
kubectl apply --dry-run=server -f istio/authorization/allow/
kubectl apply --dry-run=server -f istio/authorization/deny-final/
```

Nếu server dry-run không khả dụng:

```bash
kubectl apply --dry-run=client -f istio/mtls/
kubectl apply --dry-run=client -f istio/virtual-services/
kubectl apply --dry-run=client -f istio/authorization/allow/
kubectl apply --dry-run=client -f istio/authorization/deny-final/
```

