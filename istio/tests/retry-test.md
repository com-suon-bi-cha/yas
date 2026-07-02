# Retry Policy Test

Retry policy được cấu hình trong `istio/virtual-services/` cho các service:

- `product`
- `order`
- `payment`
- `cart`
- `inventory`

## Apply retry policies

```bash
kubectl apply -f istio/virtual-services/
kubectl get virtualservice -n dev
```

## Fault injection tạm thời cho product

Chỉ dùng để demo retry, xóa ngay sau khi test.

```bash
kubectl apply -f - <<'EOF'
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-fault-test
  namespace: dev
spec:
  hosts:
    - product.dev.svc.cluster.local
  http:
    - fault:
        abort:
          percentage:
            value: 50
          httpStatus: 500
      route:
        - destination:
            host: product.dev.svc.cluster.local
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx,reset,connect-failure,retriable-4xx
      timeout: 10s
EOF
```

## Gửi request lặp lại

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
for i in $(seq 1 30); do
  kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
    curl -s -o /dev/null -w "request $i -> %{http_code}\n" \
    http://product.dev.svc.cluster.local:8080/product/actuator/health
done
```

## Kiểm tra trong Kiali

- Graph -> Namespace `dev`.
- Chọn edge `storefront-bff -> product`.
- Xem request rate, error rate, retries nếu Kiali hiển thị.

## Dọn fault test

```bash
kubectl delete virtualservice product-fault-test -n dev
kubectl apply -f istio/virtual-services/dev-product-vs.yaml
```
