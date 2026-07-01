# Curl Test: Allowed và Denied Traffic

Chỉ chạy phần denied sau khi đã apply `istio/authorization/allow/` và `istio/authorization/deny-final/dev-deny-all.yaml`.

## Baseline trước AuthorizationPolicy

```bash
kubectl get pods -n dev

CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Kỳ vọng: không phải `403`. Nếu trả `200`, endpoint đủ tốt để làm evidence.

## Apply AuthorizationPolicy ở bước cuối

```bash
kubectl apply -f istio/authorization/allow/
kubectl apply -f istio/authorization/deny-final/dev-deny-all.yaml
```

## Allowed test

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Kỳ vọng: `200` hoặc ít nhất không phải `403`.

## Denied test

```bash
SEARCH=$(kubectl get pods -n dev -l app=search -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$SEARCH" -c search -- \
  curl -v http://payment.dev.svc.cluster.local:8081/payment/actuator/health
```

Kỳ vọng: `403`, `RBAC: access denied`, hoặc connection bị chặn bởi policy.

## Rollback

```bash
kubectl delete authorizationpolicy --all -n dev
```

