# Kiali Screenshots Checklist

## Mở Kiali

```bash
kubectl get svc kiali -n istio-system
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

Nếu dùng NodePort, lấy port:

```bash
kubectl get svc kiali -n istio-system
```

## Generate traffic

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
while true; do
  kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
    curl -s http://product.dev.svc.cluster.local:8080/product/actuator/health >/dev/null
  sleep 2
done
```

## Screenshots cần chụp

- Graph -> Namespace `dev`, service graph.
- Display -> Security, có mTLS lock.
- Display -> Traffic Animation hoặc request rate.
- Service detail của `product`.
- Workload detail của `product`.
- Retry/error evidence sau khi chạy fault injection.

