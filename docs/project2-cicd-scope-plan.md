# Project 2 - Scope CI/CD Hiện Tại Cho YAS

Tài liệu này là source of truth cho phạm vi CI/CD, GitOps và Service Mesh của đồ án Project 2 tại thời điểm cập nhật. Các kết luận dưới đây dựa trên GitOps render và trạng thái live cluster.

## Trạng Thái Hiện Tại

- Repo `gitops-manifest-k8s`: branch `main`, sạch, đồng bộ `origin/main`.
- ArgoCD:
  - `yas-dev`: `Synced/Healthy`, revision `5a74aeb`.
  - `yas-staging`: `Synced/Healthy`, revision `5a74aeb`.
- Namespace `dev`: 17 Deployment đang `1/1`, gồm 16 workload ứng dụng và `kafka-connect`.
- Namespace `staging`: 17 Deployment đang chạy, cùng scope workload với `dev`.
- Namespace `developer-build`: có 16 Deployment và NodePort Service, nhưng Deployment đang scale `0/0` cho đến khi chạy job `developer_build`.
- Service Mesh:
  - `dev` đã có `PeerAuthentication STRICT`, `DestinationRule ISTIO_MUTUAL`, `VirtualService retry`, `AuthorizationPolicy`.
  - `staging` hiện chỉ có `Gateway` và ingress `VirtualService`.
  - `developer-build` có `Gateway`, ingress `VirtualService` và DestinationRule để gọi infra ở `dev`.

## Scope Chính Thức Hiện Tại

### Workload ứng dụng trong scope deploy

Scope ứng dụng hiện tại là **16 workload**:

| Nhóm | Workload | Source path | Image | Port | Ghi chú |
|------|----------|-------------|-------|------|---------|
| Core e-commerce | `product` | `product/` | `bingsu1103/product:<tag>` | `8080` | Dữ liệu sản phẩm |
| Core e-commerce | `cart` | `cart/` | `bingsu1103/cart:<tag>` | `8084` | Giỏ hàng |
| Core e-commerce | `order` | `order/` | `bingsu1103/order:<tag>` | `8085` | Order flow |
| Core e-commerce | `customer` | `customer/` | `bingsu1103/customer:<tag>` | `8088` | Hồ sơ khách hàng |
| Core e-commerce | `inventory` | `inventory/` | `bingsu1103/inventory:<tag>` | `8090` | Tồn kho |
| Core e-commerce | `tax` | `tax/` | `bingsu1103/tax:<tag>` | `8091` | Thuế, dùng demo retry |
| Core e-commerce | `payment` | `payment/` | `bingsu1103/payment:<tag>` | `8081` | Payment provider/order payment |
| Supporting | `media` | `media/` | `bingsu1103/media:<tag>` | `8083` | Ảnh sản phẩm |
| Supporting | `search` | `search/` | `bingsu1103/search:<tag>` | `8092` | Tìm kiếm, dùng demo Authz |
| Supporting | `location` | `location/` | `bingsu1103/location:<tag>` | `8086` | Được bổ sung vì dependency thực tế |
| Frontend/BFF | `storefront-bff` | `storefront-bff/` | `bingsu1103/storefront-bff:<tag>` | `8087` | BFF cho storefront |
| Frontend/BFF | `storefront-ui` | `storefront/` | `bingsu1103/storefront:<tag>` | `3000` | UI khách hàng |
| Backoffice/BFF | `backoffice-bff` | `backoffice-bff/` | `bingsu1103/backoffice-bff:<tag>` | `8087` | BFF quản trị |
| Backoffice/BFF | `backoffice-ui` | `backoffice/` | `bingsu1103/backoffice:<tag>` | `3000` | UI quản trị |
| Tooling | `swagger-ui` | `k8s/charts/swagger-ui/` | `swaggerapi/swagger-ui` | `8080` | API documentation |
| Data seed | `sampledata` | `sampledata/` | `bingsu1103/sampledata:<tag>` | `8094` | Nạp dữ liệu mẫu |

### Workload hỗ trợ trong `dev` và `staging`

| Workload | Image | Vai trò |
|----------|-------|---------|
| `kafka-connect` | `quay.io/debezium/connect:2.4` | Chạy Debezium connector/CDC hook để đồng bộ dữ liệu sang Kafka/Search |

`kafka-connect` không tính là service ứng dụng YAS, nhưng là workload GitOps/CD đang được quản lý trong `dev` và `staging`.

### Service ngoài scope deploy hiện tại

Các service sau **không render thành Deployment trong `dev`/`staging`**:

```text
promotion
rating
delivery
recommendation
webhook
payment-paypal
```

Nếu nhóm muốn đưa một service ngoài scope vào demo, phải cập nhật đồng bộ:

1. `Jenkinsfile.ci`
2. `Jenkinsfile.developer-build`
3. `scripts/update-gitops-manifest.sh`
4. `gitops-manifest-k8s/base`
5. `gitops-manifest-k8s/environments/{dev,staging,developer-build}`
6. Istio `DestinationRule` và `AuthorizationPolicy`
7. Báo cáo và screenshot liên quan

## Kiểm Tra Scope

Trong repo `gitops-manifest-k8s`:

```bash
kubectl kustomize environments/dev > /tmp/yas-dev-rendered.yaml
kubectl kustomize environments/staging > /tmp/yas-staging-rendered.yaml

awk '/^kind: Deployment$/{in_dep=1; next} in_dep && /^metadata:/{next} in_dep && /^  name: /{sub(/^  name: /,""); print; in_dep=0}' /tmp/yas-dev-rendered.yaml | sort
grep -nE 'name: (promotion|rating|delivery|recommendation|webhook|payment-paypal)$' /tmp/yas-dev-rendered.yaml || true
```

Trên cluster:

```bash
kubectl get application yas-dev yas-staging -n argocd -o wide
kubectl get deploy,svc,sa -n dev
kubectl get deploy -n dev -o jsonpath='{range .items[*]}{.metadata.name}{" -> "}{.spec.template.spec.containers[0].image}{" ready="}{.status.readyReplicas}{"/"}{.status.replicas}{"\n"}{end}' | sort
```

Kỳ vọng hiện tại:

- `dev`: 17 Deployment, gồm 16 workload ứng dụng + `kafka-connect`.
- `staging`: 17 Deployment, gồm 16 workload ứng dụng + `kafka-connect`.
- `developer-build`: 16 Deployment, đang scale `0/0`, có NodePort Service để job `developer_build` bật khi cần.

## Trạng Thái Theo Phase

### Phase 1 - Chốt Scope

- [x] Scope ứng dụng hiện tại: 16 workload.
- [x] Bổ sung `location` so với scope 15 workload ban đầu.
- [x] Bổ sung `kafka-connect` như workload hỗ trợ GitOps/CDC.
- [x] Xác nhận `promotion`, `rating`, `delivery`, `recommendation`, `webhook`, `payment-paypal` không deploy trong `dev`/`staging`.

### Phase 2 - CI Build & Push

- [x] `Jenkinsfile.ci` đã giới hạn build/test/push theo scope hiện tại.
- [x] Có `location` trong danh sách backend/docker service.
- [x] UI images dùng `storefront` và `backoffice`.
- [x] `swagger-ui` dùng image public.
- [ ] Cần chụp Jenkins run mới nhất để đưa vào báo cáo.

### Phase 3 - Developer Build

- [x] `Jenkinsfile.developer-build` có parameter cho 16 workload ứng dụng.
- [x] `developer-build` có NodePort Service cho 16 workload.
- [x] Deployment trong `developer-build` đang scale `0/0` để tiết kiệm tài nguyên.
- [ ] Cần chạy một job demo và chụp bảng NodePort/console output nếu báo cáo yêu cầu.

### Phase 4 - GitOps Manifest

- [x] `dev` render đúng 16 workload ứng dụng + `kafka-connect`.
- [x] `staging` render đúng 16 workload ứng dụng + `kafka-connect`.
- [x] ArgoCD `yas-dev` và `yas-staging` đã kiểm tra `Synced/Healthy` tại revision `5a74aeb`.
- [ ] Trước khi chụp báo cáo, kiểm tra lại trạng thái live vì Jenkins có thể tiếp tục tạo GitOps commit mới.
- [x] `base/location` và `base/kafka-connect` đã được include trong overlay.

### Phase 5 - CD Dev/Staging

- [x] Dev sync qua ArgoCD.
- [x] Staging sync qua ArgoCD.
- [x] Các Deployment trong `dev` đang `1/1`.
- [ ] Trước khi nộp báo cáo, chụp lại sau lần Jenkins/GitOps commit cuối cùng.
- [ ] Cần bổ sung bằng chứng Jenkins build/push tag và GitOps commit cho báo cáo.

### Phase 6 - Service Mesh

- [x] Istio system chạy trong `istio-system`.
- [x] Namespace `dev` có sidecar injection.
- [x] App pod trong `dev` có `2/2` containers.
- [x] `dev` có mTLS STRICT.
- [x] `dev` có retry policy.
- [x] `dev` có AuthorizationPolicy.
- [x] Allowed test: `storefront-bff` SA gọi `product` trả `200`.
- [x] Denied test: `search` SA gọi `payment` trả `403 RBAC: access denied`.
- [x] Kiali Istio Config, Overview, Service Graph và ingress curl checks đã có screenshot trong [member4-report.md](member4-report.md).

### Phase 7 - Observability

- [x] Namespace `observability` tồn tại và đang `Active`.
- [x] Helm releases Observability đang `deployed`: `prometheus`, `grafana`, `grafana-operator`, `loki`, `tempo`, `promtail`, `opentelemetry-operator`, `opentelemetry-collector`.
- [x] Các pod chính của Prometheus/Grafana/Loki/Tempo/Promtail/OpenTelemetry đang `Running`.
- [x] Grafana Operator quản lý `Grafana` resource ở trạng thái `complete/success`.
- [x] Grafana đã có datasource Loki/Tempo và dashboard JVM/Hikari theo custom resources.
- [x] OpenTelemetry Collector `READY 1/1`, nhận OTLP trên `4317`/`4318`, gửi metrics sang Prometheus và traces sang Tempo.
- [ ] Cần chụp thêm minh chứng rút gọn cho Observability: Helm releases, pod/service trong namespace `observability`, Grafana datasources, dashboard metrics và Tempo tracing.
- [x] Khung báo cáo bổ sung nằm ở [observability-report.md](observability-report.md).

## Demo Check Nhanh

Port-forward Istio ingress:

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 18080:80
```

Kiểm tra từ máy local:

```bash
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/
curl -H 'Host: swagger.dev.yas.local.com' http://127.0.0.1:18080/swagger-ui/
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/product/storefront/categories
curl -H 'Host: storefront.dev.yas.local.com' http://127.0.0.1:18080/api/payment/storefront/payment-providers
```

Kết quả đã kiểm tra gần nhất:

- Storefront `/`: `200`
- Swagger `/swagger-ui/`: `200`
- Product categories: `200`
- Payment providers: `200`, body `[]`

## Ghi Chú Báo Cáo

Khi viết báo cáo LaTeX, nên mô tả scope là:

> Hệ thống hiện triển khai 16 workload ứng dụng YAS trong `dev`/`staging`, bổ sung `location` so với scope ban đầu do dependency thực tế. Ngoài ra, namespace còn có `kafka-connect` như workload hỗ trợ CDC/GitOps hook, không tính là service ứng dụng chính.
