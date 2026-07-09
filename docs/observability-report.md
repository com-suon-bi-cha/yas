# Báo Cáo Observability - Prometheus, Grafana, Loki, Tempo Và OpenTelemetry

## 1. Tóm Tắt Phần Việc

Nhóm đã triển khai bổ sung Observability cho cluster YAS trong namespace `observability`. Stack hiện tại gồm Prometheus để lưu metrics, Grafana để quan sát dashboard, Loki/Promtail để thu thập log, Tempo để lưu distributed trace và OpenTelemetry Collector để nhận telemetry từ các service. Các minh chứng trong báo cáo được rút gọn theo đúng phạm vi đã triển khai và có thể chụp lại từ hệ thống hiện tại: Helm releases, pod/service trong namespace `observability`, Grafana datasources, dashboard metrics và tracing.

Phần này tách riêng khỏi phần Istio/Kiali của thành viên khác.

## 2. Kiến Trúc Observability

Luồng tổng quát:

```text
YAS services / Kubernetes cluster
    -> OpenTelemetry Collector / Prometheus metrics
    -> Prometheus, Loki, Tempo
    -> Grafana datasources
    -> Grafana dashboards / tracing view
```

Sơ đồ có sẵn trong docs:

![YAS Observability](images/yas-observability.png)

Caption: Kiến trúc Observability của YAS sử dụng OpenTelemetry Collector để gom telemetry, Prometheus cho metrics, Loki cho logs, Tempo cho traces và Grafana để quan sát tập trung.

## 3. Cấu Hình Triển Khai

Các file cấu hình chính trong repo YAS:

- `k8s/deploy/setup-cluster.sh`: cài Promtail, Prometheus, Grafana Operator và chart Grafana datasource/dashboard.
- `k8s/deploy/observability/prometheus.values.yaml`: cấu hình `kube-prometheus-stack` và Grafana đi kèm.
- `k8s/deploy/observability/loki.values.yaml`: cấu hình Loki.
- `k8s/deploy/observability/tempo.values.yaml`: cấu hình Tempo.
- `k8s/deploy/observability/promtail.values.yaml`: cấu hình Promtail.
- `k8s/deploy/observability/opentelemetry/`: chart tạo `OpenTelemetryCollector`.
- `k8s/deploy/observability/grafana/`: chart tạo Grafana datasource và dashboard.

Lệnh kiểm chứng:

```bash
helm list -n observability
kubectl get pods,svc -n observability
kubectl get grafana,grafanadatasource,grafanadashboard -n observability
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/01-observability-helm-releases.png
```

Caption: Các Helm release của Observability trong namespace `observability` đều ở trạng thái `deployed`, gồm Prometheus, Grafana, Loki, Tempo, Promtail và OpenTelemetry.

## 4. Kết Quả Kiểm Tra Cluster

Kết quả đã kiểm chứng:

- Namespace `observability` tồn tại và đang `Active`.
- Helm releases đã `deployed`: `prometheus`, `grafana`, `grafana-operator`, `loki`, `tempo`, `promtail`, `opentelemetry-operator`, `opentelemetry-collector`.
- Các pod chính đang `Running`: Prometheus, Grafana, Loki backend/read/write/gateway, Tempo, Promtail, OpenTelemetry Collector và OpenTelemetry Operator.
- Các service chính đã được expose nội bộ để Grafana truy vấn dữ liệu từ Prometheus, Loki và Tempo.

Screenshot khuyến nghị:

```text
docs/images/observability-report/02-observability-pods-services.png
```

Caption: Các pod và service của Observability trong namespace `observability` đang chạy, chứng minh stack metrics/logs/traces đã được triển khai trên cluster.

## 5. Grafana Datasources

Grafana được cấu hình để dùng chung các nguồn dữ liệu quan sát của hệ thống. Các datasource cần thể hiện trong ảnh gồm:

- Prometheus: dùng cho metrics và dashboard.
- Loki: dùng cho log tập trung.
- Tempo: dùng cho distributed tracing.

Lệnh kiểm chứng:

```bash
kubectl get grafanadatasource -n observability
```

Truy cập Grafana:

```text
http://grafana.yas.local.com:31255
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/03-grafana-datasources.png
```

Caption: Grafana hiển thị các datasource Prometheus, Loki và Tempo, chứng minh dữ liệu metrics/logs/traces được gom về cùng một giao diện quan sát.

## 6. Grafana Dashboard Metrics

Grafana dashboard dùng để quan sát metrics runtime của service Java trong hệ thống YAS. Khi chụp ảnh, chọn namespace `dev` hoặc `staging`, sau đó chọn một service backend có traffic như `product`, `customer`, `order`, `storefront-bff` hoặc `backoffice-bff`.

Nguồn dashboard nằm trong repo:

- `docker/grafana/provisioning/dashboards/observability_dashboard.json`
- `docker/grafana/provisioning/dashboards/opentelemetry-collector.json`
- `docker/grafana/provisioning/dashboards/prometheus-dashboard.json`

Screenshot khuyến nghị:

```text
docs/images/observability-report/04-grafana-dashboard-metrics.png
```

Caption: Grafana dashboard hiển thị metrics runtime của service Java, dùng để theo dõi CPU, memory, thread, HTTP request và JVM health.

## 7. Grafana Tracing

Tempo được dùng để lưu distributed traces. OpenTelemetry Collector nhận trace từ các service rồi gửi sang Tempo, sau đó Grafana đọc dữ liệu trace thông qua datasource Tempo.

Để có trace trước khi chụp ảnh, tạo traffic từ storefront hoặc backoffice, ví dụ: tìm kiếm sản phẩm, mở chi tiết sản phẩm, tạo địa chỉ, checkout hoặc gọi API qua Swagger. Sau đó vào Grafana Explore, chọn datasource `Tempo` và tìm trace gần nhất.

Screenshot khuyến nghị:

```text
docs/images/observability-report/05-grafana-tempo-tracing.png
```

Caption: Grafana Tempo hiển thị trace của request, giúp theo dõi luồng xử lý giữa các service và hỗ trợ phân tích độ trễ.

## 8. Kết Luận

Observability stack đã được triển khai thành công ở mức hạ tầng cluster và có thể quan sát thông qua Grafana. Bộ ảnh minh chứng được rút gọn theo phạm vi thực tế gồm Helm releases, trạng thái pod/service, Grafana datasources, dashboard metrics và tracing. Các phần như Prometheus target chi tiết, Loki log explore riêng hoặc cấu hình OpenTelemetry Collector chi tiết không bắt buộc chụp trong báo cáo cuối nếu nhóm chỉ trình bày theo bộ screenshot hiện có.
