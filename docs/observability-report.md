# Báo Cáo Observability - Prometheus, Grafana, Loki, Tempo Và OpenTelemetry

## 1. Tóm Tắt Phần Việc

Nhóm đã triển khai bổ sung Observability cho cluster YAS trong namespace `observability`. Stack hiện tại gồm Prometheus/Kube Prometheus Stack để thu thập metrics, Grafana để quan sát dashboard, Loki/Promtail để thu thập log, Tempo để lưu trace và OpenTelemetry Collector để nhận dữ liệu telemetry. Các resource chính đã được kiểm tra trên cluster ngày 2026-07-09 và đang ở trạng thái `Running`/`deployed`. Phần này dùng để bổ sung minh chứng cho báo cáo cuối cùng, tách riêng khỏi phần Istio/Kiali của TV4.

## 2. Kiến Trúc Observability

Luồng tổng quát:

```text
YAS services / Kubernetes cluster
    -> Spring Actuator Prometheus endpoint / kube-state-metrics / node-exporter
    -> Prometheus
    -> Grafana dashboards

Application logs / pod logs
    -> Promtail
    -> OpenTelemetry Collector / Loki endpoint
    -> Loki
    -> Grafana Explore

Application traces
    -> OpenTelemetry Collector
    -> Tempo
    -> Grafana Trace / Node Graph
```

Sơ đồ có sẵn trong docs:

![YAS Observability](images/yas-observability.png)

Caption: Kiến trúc Observability của YAS sử dụng OpenTelemetry Collector để gom telemetry, Prometheus cho metrics, Loki cho logs, Tempo cho traces và Grafana để quan sát tập trung.

## 3. Cấu Hình Triển Khai

Các file cấu hình chính trong repo YAS:

- `k8s/deploy/setup-cluster.sh`: cài Promtail, Prometheus, Grafana Operator và chart Grafana datasource/dashboard.
- `k8s/deploy/observability/prometheus.values.yaml`: cấu hình `kube-prometheus-stack` và Grafana đi kèm.
- `k8s/deploy/observability/loki.values.yaml`: cấu hình Loki.
- `k8s/deploy/observability/tempo.values.yaml`: bật Tempo metrics generator và remote write về Prometheus.
- `k8s/deploy/observability/promtail.values.yaml`: cấu hình Promtail gửi log về OpenTelemetry Collector/Loki endpoint.
- `k8s/deploy/observability/opentelemetry/`: chart tạo `OpenTelemetryCollector`.
- `k8s/deploy/observability/grafana/`: chart tạo Grafana datasource và dashboard.

Lệnh kiểm chứng:

```bash
helm list -n observability
kubectl get pods,svc -n observability
kubectl get grafana,grafanadatasource,grafanadashboard -n observability
kubectl get opentelemetrycollector -n observability -o wide
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
- Service `opentelemetry-collector` mở các port `3500`, `4317`, `4318`; service `prometheus-kube-prometheus-prometheus` mở port `9090`; service `prometheus-grafana` mở port `80`; service `tempo` mở các port trace gồm `4317` và `4318`; service `loki-gateway` mở port `80`.

Screenshot khuyến nghị:

```text
docs/images/observability-report/02-observability-pods-services.png
```

Caption: Các pod và service của Observability trong namespace `observability` đang chạy, chứng minh stack metrics/logs/traces đã được triển khai trên cluster.

## 5. Prometheus Metrics

Prometheus được cài thông qua `kube-prometheus-stack`. Stack này tạo Prometheus server, Alertmanager, kube-state-metrics, node-exporter, Prometheus Operator, ServiceMonitor và PrometheusRule mặc định cho Kubernetes.

Kết quả đã kiểm chứng:

- `prometheus-prometheus-kube-prometheus-prometheus-0`: `2/2 Running`.
- `prometheus-kube-state-metrics`: `1/1 Running`.
- `prometheus-prometheus-node-exporter`: `1/1 Running`.
- Nhiều `ServiceMonitor` và `PrometheusRule` đã được tạo trong namespace `observability`.
- Nhiều workload backend trong namespace `dev` có `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus`, giúp expose endpoint metrics qua Spring Actuator.

Lệnh kiểm chứng:

```bash
kubectl get servicemonitor,prometheusrule -n observability
kubectl get deploy -n dev -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{range .spec.template.spec.containers[0].env[*]}{.name}{"="}{.value}{";"}{end}{"\n"}{end}' | rg 'MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE|prometheus'
kubectl port-forward -n observability svc/prometheus-kube-prometheus-prometheus 9090:9090
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/03-prometheus-targets-or-query.png
```

Caption: Prometheus hiển thị targets/queries hoạt động, dùng để chứng minh hệ thống đang thu thập metrics từ Kubernetes và các service có endpoint Prometheus.

## 6. Grafana Dashboard

Grafana được quản lý bằng Grafana Operator. Resource `Grafana` hiện có stage `complete` và stage status `success`. Operator đã ghi nhận datasources `loki-datasource`, `tempo-datasource` và dashboards `jvm-dashboard`, `hikari-cp-dashboard`.

Lệnh kiểm chứng:

```bash
kubectl get grafana grafana -n observability -o yaml
kubectl get grafanadatasource,grafanadashboard -n observability
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Truy cập Grafana:

```text
http://localhost:3000
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/04-grafana-datasources.png
docs/images/observability-report/05-grafana-jvm-dashboard.png
```

Caption `04`: Grafana hiển thị các datasource Prometheus, Loki và Tempo, chứng minh dữ liệu metrics/logs/traces được gom về cùng một giao diện quan sát.

Caption `05`: JVM dashboard trong Grafana hiển thị metrics runtime của service Java, dùng để theo dõi CPU, memory, thread và JVM health.

## 7. Loki Và Log

Loki được cài trong namespace `observability` với các thành phần backend, read, write, gateway, MinIO và cache. Promtail chạy dạng agent để thu thập log pod và gửi về endpoint Loki/OpenTelemetry theo cấu hình `promtail.values.yaml`.

Kết quả đã kiểm chứng:

- `loki-backend`, `loki-read`, `loki-write`, `loki-gateway` đều `Running`.
- `promtail` đang `Running`.
- Grafana datasource `loki-datasource` trỏ tới `http://loki-gateway`.
- Datasource Loki có derived field `traceId` liên kết sang Tempo.

Lệnh kiểm chứng:

```bash
kubectl get pods,svc -n observability | rg 'loki|promtail'
kubectl get grafanadatasource loki-datasource -n observability -o yaml
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/06-grafana-loki-logs.png
```

Caption: Grafana Explore với datasource Loki hiển thị log của pod/service trong namespace `dev`, dùng để chứng minh log đã được thu thập tập trung.

## 8. Tempo Và Trace

Tempo được cài trong namespace `observability` và expose các port trace phổ biến, gồm OTLP gRPC/HTTP `4317`/`4318`. OpenTelemetry Collector nhận trace qua OTLP và export sang Tempo theo endpoint `http://tempo:4318`. Grafana datasource `tempo-datasource` bật Node Graph, Loki search và liên kết trace-to-logs.

Kết quả đã kiểm chứng:

- Pod `tempo-0`: `1/1 Running`.
- Service `tempo` expose `4317/TCP`, `4318/TCP`, `3200/TCP` và các port trace khác.
- OpenTelemetry Collector CR ở mode `deployment`, version `0.153.0`, `READY 1/1`.
- Pipeline live của collector có `traces` receiver `otlp` và exporter `otlphttp` tới Tempo.

Lệnh kiểm chứng:

```bash
kubectl get pod,svc -n observability | rg 'tempo|opentelemetry'
kubectl describe opentelemetrycollector opentelemetry -n observability
kubectl get grafanadatasource tempo-datasource -n observability -o yaml
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/07-grafana-tempo-traces.png
docs/images/observability-report/08-grafana-tempo-node-graph.png
```

Caption `07`: Tempo trong Grafana hiển thị trace của request, giúp theo dõi luồng xử lý giữa các service.

Caption `08`: Tempo Node Graph hiển thị quan hệ giữa các service trong một trace, hỗ trợ phân tích độ trễ và dependency.

## 9. OpenTelemetry Collector

OpenTelemetry Collector là điểm gom dữ liệu telemetry. Cấu hình live đã kiểm chứng cho thấy collector nhận OTLP trên `0.0.0.0:4317` và `0.0.0.0:4318`; metrics được remote write về Prometheus, traces được export sang Tempo. Service `opentelemetry-collector` cũng expose port `3500` cho luồng log/Loki theo cấu hình chart.

Lệnh kiểm chứng:

```bash
kubectl get opentelemetrycollector -n observability -o wide
kubectl describe opentelemetrycollector opentelemetry -n observability
kubectl get svc opentelemetry-collector -n observability
```

Screenshot khuyến nghị:

```text
docs/images/observability-report/09-opentelemetry-collector-config.png
```

Caption: OpenTelemetry Collector ở trạng thái `READY 1/1`, expose OTLP gRPC/HTTP và cấu hình pipeline gửi metrics sang Prometheus, traces sang Tempo.

## 10. Kết Luận

Observability stack đã được triển khai thành công ở mức hạ tầng cluster. Prometheus/Kube Prometheus Stack cung cấp metrics Kubernetes và nền tảng alert rule; Grafana là giao diện tập trung để xem dashboard; Loki/Promtail phục vụ log tập trung; Tempo phục vụ distributed tracing; OpenTelemetry Collector đóng vai trò nhận và chuyển tiếp telemetry. Khi viết báo cáo LaTeX, nhóm nên chèn thêm screenshot UI Grafana cho datasources, dashboard metrics, Loki logs và Tempo traces để chứng minh không chỉ pod chạy mà dữ liệu quan sát cũng có thể xem được.
