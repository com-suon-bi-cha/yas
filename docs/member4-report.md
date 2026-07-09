# Báo Cáo TV4 - Istio Service Mesh, Security Và Kiali

## 1. Tóm Tắt Phần Việc

TV4 phụ trách triển khai và kiểm chứng Service Mesh cho YAS bằng Istio. Nội dung gồm ingress routing, mTLS, AuthorizationPolicy, retry policy, Kiali visualization và tổng hợp bằng chứng.

Viết 1 đoạn 5-8 câu:

- Istio giúp kiểm soát traffic service-to-service.
- `dev` là môi trường bật đầy đủ mTLS/Authz/retry.
- `staging` hiện mới có ingress Gateway/VirtualService.
- AuthorizationPolicy dựa trên ServiceAccount.
- Kiali dùng để quan sát topology và mTLS.

## 2. Cấu Hình Istio Trong GitOps

Nội dung viết:

- `base/istio/gateway.yaml`: tạo `yas-gateway`.
- `base/istio/virtualservice.yaml`: route ingress cho storefront, backoffice, identity, swagger và BFF.
- `environments/dev/istio/mtls.yaml`: mTLS STRICT và DestinationRule.
- `environments/dev/istio/retry.yaml`: retry policy.
- `environments/dev/istio/authorization.yaml`: AuthorizationPolicy.

Screenshot cần chèn:

```markdown
![GitOps Istio Files](images/member4-report/01-gitops-istio-files.png)
Caption: Các manifest Istio được quản lý trong repo gitops-manifest-k8s.
```

## 3. Istio System Và Sidecar

Lệnh kiểm chứng:

```bash
kubectl get pods -n istio-system
kubectl get ns dev staging developer-build --show-labels
kubectl get pods -n dev
```

Screenshot cần chèn:

```markdown
![Istio System Pods](images/member4-report/02-istio-system-pods.png)
Caption: Các thành phần Istio, Kiali, Prometheus và Grafana đang Running.
```

```markdown
![Dev Pods Sidecar](images/member4-report/03-dev-pods-sidecar.png)
Caption: Pod trong namespace dev có trạng thái 2/2, chứng minh sidecar đã được inject.
```

## 4. mTLS STRICT

Lệnh kiểm chứng:

```bash
kubectl get peerauthentication -n dev
kubectl get destinationrule -n dev
kubectl describe peerauthentication dev-strict-mtls -n dev
```

Screenshot cần chèn:

```markdown
![PeerAuthentication Strict](images/member4-report/04-peer-authentication-strict.png)
Caption: PeerAuthentication dev-strict-mtls bật mTLS STRICT cho namespace dev.
```

```markdown
![DestinationRules](images/member4-report/05-destination-rules.png)
Caption: DestinationRule cấu hình ISTIO_MUTUAL cho các app service trong scope.
```

Nội dung viết:

- Không dùng wildcard quá rộng để tránh ảnh hưởng infra.
- DestinationRule chỉ áp dụng cho service có sidecar.

## 5. Retry Policy

Lệnh kiểm chứng:

```bash
kubectl get virtualservice -n dev
kubectl describe virtualservice tax-retry -n dev
```

Screenshot cần chèn:

```markdown
![VirtualService Retry](images/member4-report/06-virtualservice-retry.png)
Caption: VirtualService tax-retry cấu hình attempts=3, perTryTimeout=2s và retryOn cho lỗi tạm thời.
```

Nội dung viết:

- Retry áp dụng cho các service quan trọng: `product`, `cart`, `order`, `tax`, `payment`, `inventory`.
- Mục tiêu là tăng khả năng chịu lỗi tạm thời trong service-to-service traffic.

## 6. AuthorizationPolicy

Lệnh kiểm chứng:

```bash
kubectl get authorizationpolicy -n dev
kubectl describe authorizationpolicy allow-to-payment -n dev
```

Screenshot cần chèn:

```markdown
![Authorization Policies](images/member4-report/07-authorization-policies.png)
Caption: Namespace dev có AuthorizationPolicy kiểm soát traffic vào từng workload.
```

```markdown
![Allow To Payment](images/member4-report/08-allow-to-payment.png)
Caption: Policy allow-to-payment chỉ cho storefront-bff, backoffice-bff, order và ingress gateway gọi payment.
```

## 7. Test Allow/Deny

Lệnh test:

```bash
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
```

Kết quả đã kiểm chứng:

- `storefront-bff` SA -> `product`: `200`
- `search` SA -> `payment`: `403 RBAC: access denied`

Screenshot cần chèn:

```markdown
![Allowed Curl 200](images/member4-report/09-allowed-curl-200.png)
Caption: Request hợp lệ từ storefront-bff tới product trả về 200.
```

```markdown
![Denied Curl 403](images/member4-report/10-denied-curl-403.png)
Caption: Request không hợp lệ từ search tới payment bị Istio chặn với RBAC 403.
```

## 8. Kiali Visualization

Mở Kiali:

```bash
kubectl port-forward -n istio-system svc/kiali 20001:20001
```

Truy cập:

```text
http://localhost:20001
```

Screenshot cần chèn:

```markdown
![Kiali Graph](images/member4-report/11-kiali-graph.png)
Caption: Kiali graph thể hiện topology traffic giữa các service trong namespace dev.
```

```markdown
![Kiali Security](images/member4-report/12-kiali-security.png)
Caption: Kiali Security view hiển thị mTLS lock cho các kết nối trong mesh.
```

```markdown
![Kiali Workload Metrics](images/member4-report/13-kiali-workload-metrics.png)
Caption: Metrics của workload trong Kiali dùng để quan sát traffic và lỗi.
```

## 9. Ingress Demo

Lệnh kiểm chứng:

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 18080:80

curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/
curl -H 'Host: swagger.dev.yas.local.com' http://127.0.0.1:18080/swagger-ui/
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/product/storefront/categories
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/payment/storefront/payment-providers
```

Kết quả đã kiểm chứng:

- Storefront: `200`
- Swagger: `200`
- Product categories: `200`
- Payment providers: `200`, body `[]`

Screenshot cần chèn:

```markdown
![Ingress Curl Checks](images/member4-report/14-ingress-curl-checks.png)
Caption: Các endpoint chính truy cập qua Istio ingress trả về HTTP 200.
```

## 10. Kết Luận

Kết luận cần nêu:

- `dev` đã có Service Mesh đầy đủ.
- mTLS, retry và AuthorizationPolicy đã được quản lý bằng GitOps.
- Kiali cung cấp bằng chứng trực quan cho topology và security.
- `staging` hiện mới có ingress Istio; nếu muốn bật policy như `dev`, cần bổ sung overlay `environments/staging/istio`.
