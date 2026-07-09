# TV4 - Service Mesh, Security, Kiali Và Tổng Hợp Báo Cáo

## Vai Trò

TV4 chịu trách nhiệm phần Service Mesh và bằng chứng nâng cao:

- Cấu hình Istio trong GitOps.
- mTLS STRICT cho `dev`.
- `DestinationRule ISTIO_MUTUAL`.
- `AuthorizationPolicy` allow/deny theo ServiceAccount.
- `VirtualService` retry.
- Kiali graph/security/traffic.
- Tổng hợp bằng chứng từ TV1/TV2/TV3 để viết báo cáo cuối.

## Scope Service Mesh Hiện Tại

### `dev`

`dev` hiện đã có:

- 1 `Gateway`: `yas-gateway`
- 7 `VirtualService`:
  - `yas-ingress-vs`
  - `product-retry`
  - `cart-retry`
  - `order-retry`
  - `tax-retry`
  - `payment-retry`
  - `inventory-retry`
- 1 `PeerAuthentication`: `dev-strict-mtls`
- 15 `DestinationRule ISTIO_MUTUAL`
- 20 `AuthorizationPolicy`

### `staging`

`staging` hiện có:

- `yas-gateway`
- `yas-ingress-vs`

Chưa bật đầy đủ mTLS/retry/AuthorizationPolicy như `dev`.

### `developer-build`

`developer-build` hiện có:

- `yas-gateway`
- `yas-ingress-vs`
- 6 `DestinationRule` để gọi infra ở `dev`

Không cấu hình AuthorizationPolicy riêng trong giai đoạn này.

## Trạng Thái Hiện Tại

- [x] Istio system Running.
- [x] Kiali Running.
- [x] Prometheus/Grafana Running.
- [x] App pods trong `dev` có sidecar `2/2`.
- [x] ArgoCD quản lý Istio resources trong `dev`.
- [x] `tax-retry` có retry `attempts=3`, `perTryTimeout=2s`.
- [x] Test allowed: `storefront-bff` SA gọi `product` trả `200`.
- [x] Test denied: `search` SA gọi `payment` trả `403 RBAC: access denied`.
- [ ] Cần chụp Kiali graph/security/traffic cho báo cáo.

## Checklist Công Việc

### 1. Verify Istio System

```bash
kubectl get pods -n istio-system
kubectl get svc -n istio-system
```

Screenshot cần chụp:

- `istiod`
- `istio-ingressgateway`
- `kiali`
- `prometheus`
- `grafana`

### 2. Verify Sidecar Injection

```bash
kubectl get ns dev staging developer-build --show-labels
kubectl get pods -n dev
kubectl get pods -n dev -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{.spec.containers[*].name}{"\n"}{end}'
```

Screenshot cần chụp:

- Namespace có `istio-injection=enabled`.
- Pod ứng dụng `2/2`.

### 3. Verify mTLS

```bash
kubectl get peerauthentication -n dev
kubectl get destinationrule -n dev
kubectl describe peerauthentication dev-strict-mtls -n dev
```

Nội dung báo cáo:

- `PeerAuthentication` bật `STRICT`.
- `DestinationRule` dùng `ISTIO_MUTUAL` cho app services.
- Không dùng wildcard cho toàn bộ namespace để tránh chặn nhầm infra.

### 4. Verify Retry

```bash
kubectl get virtualservice -n dev
kubectl describe virtualservice tax-retry -n dev
```

Nội dung cần ghi:

- `attempts: 3`
- `perTryTimeout: 2s`
- `retryOn: 5xx,reset,connect-failure,retriable-4xx`
- `timeout: 10s`

### 5. Verify AuthorizationPolicy

```bash
kubectl get authorizationpolicy -n dev
kubectl describe authorizationpolicy allow-to-payment -n dev
```

Test allowed/denied bằng pod tạm:

```bash
kubectl delete pod curl-allowed curl-denied -n dev --ignore-not-found

kubectl run curl-allowed -n dev --image=curlimages/curl:8.8.0 --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"storefront-bff","containers":[{"name":"curl","image":"curlimages/curl:8.8.0","command":["sleep","120"]}]}}'

kubectl run curl-denied -n dev --image=curlimages/curl:8.8.0 --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"search","containers":[{"name":"curl","image":"curlimages/curl:8.8.0","command":["sleep","120"]}]}}'

kubectl wait --for=condition=Ready pod/curl-allowed pod/curl-denied -n dev --timeout=120s

kubectl exec -n dev curl-allowed -c curl -- \
  curl -s -o /tmp/out -w '%{http_code}\n' \
  http://product.dev.svc.cluster.local:8080/product/actuator/health

kubectl exec -n dev curl-denied -c curl -- \
  curl -s -o /tmp/out -w '%{http_code}\n' \
  http://payment.dev.svc.cluster.local:8081/payment/actuator/health

kubectl delete pod curl-allowed curl-denied -n dev --wait=false
```

Kết quả mong đợi:

- Allowed: `200`
- Denied: `403`, body `RBAC: access denied`

### 6. Kiali

Mở Kiali:

```bash
kubectl port-forward -n istio-system svc/kiali 20001:20001
```

Truy cập:

```text
http://localhost:20001
```

Screenshot cần chụp:

- Graph namespace `dev`.
- Security display có mTLS lock.
- Traffic animation.
- Workload detail cho `product` hoặc `tax`.
- Metrics/retry nếu có traffic.

### 7. Ingress Demo

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 18080:80
```

```bash
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/
curl -H 'Host: swagger.dev.yas.local.com' http://127.0.0.1:18080/swagger-ui/
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/product/storefront/categories
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/payment/storefront/payment-providers
```

Kết quả đã kiểm tra:

- Storefront: `200`
- Swagger: `200`
- Product categories: `200`
- Payment providers: `200`, body `[]`

## Deliverables Cho Báo Cáo

- Mô tả Service Mesh architecture.
- Mô tả mTLS STRICT.
- Mô tả AuthorizationPolicy theo ServiceAccount.
- Mô tả retry policy.
- Mô tả Kiali observability.
- Screenshot minh chứng theo [member4-report.md](member4-report.md).
