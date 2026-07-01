# TV4 — Service Mesh (Istio) + Kiali + Báo Cáo

> Vai trò: TV4 phụ trách phần Nâng cao 2: cấu hình Service Mesh cho YAS trên Kubernetes, gồm mTLS, AuthorizationPolicy, retry policy, Kiali topology, test evidence và tổng hợp báo cáo.
>
> Mục tiêu thực tế: chứng minh các microservice trong YAS giao tiếp qua Istio sidecar, được mã hóa bằng mTLS, bị giới hạn bởi policy service-to-service, có retry khi lỗi 5xx, và có hình/topology/test log đưa vào báo cáo.

---

## 1. TV4 Cần Hiểu Gì Trong Kiến Trúc Chung

### 1.1 Vị trí của TV4 trong flow tổng thể

TV1 dựng cluster và hạ tầng. TV2 build/push image và deploy bằng Jenkins/GitOps. TV3 tạo Kubernetes manifests cho 19 services. TV4 chèn lớp Service Mesh vào sau khi workload đã có trên Kubernetes.

```text
GitHub -> Jenkins -> Docker Hub -> GitOps repo -> ArgoCD -> K8s workloads
                                                          |
                                                          v
                                            Istio sidecar + mTLS + policies
                                                          |
                                                          v
                                                    Kiali topology
```

TV4 không build image, không viết Jenkins pipeline, không tạo Deployment/Service chính cho app. TV4 làm các manifest và test liên quan đến Istio:

- `PeerAuthentication`: bật mTLS.
- `DestinationRule`: yêu cầu traffic nội bộ dùng `ISTIO_MUTUAL`.
- `AuthorizationPolicy`: chặn mặc định và cho phép các cặp service được gọi nhau.
- `VirtualService`: cấu hình retry/timeout.
- Kiali: quan sát topology, mTLS, traffic, retry evidence.
- Báo cáo: tổng hợp screenshot/logs của team.

### 1.2 Kiến thức cần nắm

| Chủ đề | Cần hiểu | Dùng vào việc gì |
|---|---|---|
| Kubernetes namespace | `dev`, `staging`, `developer-build`, `istio-system` tách riêng workload | Biết apply policy vào namespace nào |
| ServiceAccount | Mỗi pod chạy dưới một identity riêng, ví dụ `cluster.local/ns/dev/sa/product` | Istio AuthorizationPolicy dựa vào identity này để allow/deny |
| Pod labels | `selector.matchLabels.app=<service>` phải khớp với label trên pod | Policy phải target đúng service đích |
| Istio sidecar injection | Mỗi pod cần thêm container `istio-proxy` | Nếu pod chỉ `1/1` thì mTLS/Authz có thể không hoạt động đúng |
| mTLS | Envoy sidecar mã hóa và xác thực service-to-service traffic | Đáp ứng yêu cầu "Enable mTLS" |
| AuthorizationPolicy | Default deny, sau đó allow theo source principal và target label | Đáp ứng yêu cầu "chỉ service được phép mới connect được" |
| VirtualService retry | Retry request khi gặp `5xx`, `reset`, `connect-failure` | Đáp ứng yêu cầu retryable |
| Kiali | Đọc metrics từ Prometheus để vẽ graph/topology | Lấy screenshot topology và security padlock |

### 1.3 Phạm vi nên làm

Làm Service Mesh trên namespace `dev` trước. Sau khi `dev` ổn định, có thể copy/patch sang `staging`. Không nên áp dụng policy chặt vào `developer-build` trong lần đầu vì job này phục vụ test branch linh hoạt, nếu policy sai sẽ làm TV2 khó debug.

Khuyến nghị:

- Bật sidecar injection cho `dev` và `staging`.
- Test mTLS/Authz/Retry trên `dev`.
- Chưa bật deny-all cho `developer-build` trừ khi team đã thống nhất.
- Báo cáo tập trung evidence trên `dev`; nếu kịp thì thêm screenshot staging.

---

## 2. Độ Phụ Thuộc Với Các Thành Viên Khác

### 2.1 Dependency matrix

| Dependency | Cần từ ai | TV4 cần nhận được | Cách kiểm tra | Nếu bị thiếu thì blocker gì |
|---|---|---|---|---|
| K3s cluster ready | TV1 | Kubeconfig hoặc quyền SSH vào VM | `kubectl get nodes` | Không cài được Istio/Kiali |
| Namespace có sẵn | TV1 | `dev`, `staging`, `developer-build`, `istio-system` | `kubectl get ns` | Không có nơi để label/apply policy |
| Firewall cho Kiali | TV1 | Mở port NodePort hoặc cho phép port-forward | `kubectl patch svc kiali ...`, truy cập UI | Không chụp được Kiali UI từ máy cá nhân |
| App pods chạy trong `dev` | TV1 + TV2 + TV3 | YAS services được deploy và Running | `kubectl get pods -n dev` | Không có traffic để test mesh |
| ServiceAccount cho từng service | TV3 | Mỗi Deployment có `serviceAccountName`, mỗi service có `ServiceAccount` riêng | `kubectl get sa -n dev` và `kubectl get deploy -n dev -o yaml` | AuthzPolicy không đúng identity, test 200/403 sai |
| Label pod thống nhất | TV3 | Pod label `app=<service>` khớp service name | `kubectl get pods -n dev --show-labels` | Selector trong policy không match target |
| Service name và port đúng | TV3 | Service DNS/port đúng với bảng port YAS | `kubectl get svc -n dev` | Curl test sai endpoint |
| Deploy sau khi label injection | TV1 + TV3 | Label namespace trước khi ArgoCD deploy/restart pod | `kubectl get pod -n dev` thấy `2/2` | Pod không có sidecar, mesh không áp dụng |
| Jenkins/GitOps đã sync image mới | TV2 | Image chạy được để tạo traffic | `kubectl rollout status deploy/<svc> -n dev` | Kiali không có topology thực tế |
| Test endpoint khả dụng | TV2/TV3 hỗ trợ | Biết URL nào trả 200 cho từng service | `curl http://product.dev.svc.cluster.local:8080/...` | Retry/Authz test khó chứng minh |

### 2.2 Các mốc cần trao đổi sớm

| Thời điểm | Cần nói với ai | Nội dung cần chốt |
|---|---|---|
| Trước khi TV3 tạo manifests | TV3 | Bắt buộc có `ServiceAccount` riêng cho 19 services và `serviceAccountName` trong Deployment |
| Trước khi TV1/ArgoCD deploy app | TV1 + TV3 | Label `dev`/`staging` bằng `istio-injection=enabled` trước deploy, hoặc chấp nhận rollout restart |
| Trước khi apply deny-all | Cả team | Thông báo thời điểm apply vì có thể làm app mất kết nối nếu allow rules chưa đủ |
| Trước khi test retry | TV2/TV3 | Chốt endpoint nào để gọi lặp lại và cách tạo lỗi 500/fault injection |
| Trước khi viết báo cáo | TV1/TV2/TV3 | Xin screenshot/log theo checklist báo cáo |

### 2.3 Câu hỏi cần đặt cho team

1. TV3 có đảm bảo mỗi service có `ServiceAccount` cùng tên service không?
2. Pod label có đúng format `app=<service>` không?
3. TV1 đã label namespace `dev`/`staging` trước khi ArgoCD sync chưa?
4. TV2/TV3 có endpoint nào chắc chắn trả 200 để dùng làm allowed test không?
5. Team muốn policy áp dụng cho `dev` trước hay cả `staging`?
6. Có cho phép cài Prometheus sample addon của Istio để Kiali có đủ metrics không? Lưu ý đây không phải phần Observability đầy đủ của đồ án, chỉ phục vụ Kiali/Service Mesh.

---

## 3. Service Communication Map Để Viết AuthorizationPolicy

Dùng map trong `docs/project2-plan.md` làm whitelist ban đầu. Tuy nhiên, khi bật deny-all thì policy phải theo traffic thực tế. Trước khi nộp, TV4 cần đối chiếu thêm `yas.services.*` trong source và Kiali graph để cập nhật allow rules nếu phát hiện flow hợp lệ bị chặn.

```text
storefront-bff  -> product, media, cart, order, customer, rating,
                  search, promotion, tax, location
backoffice-bff  -> product, media, order, inventory, promotion,
                  rating, webhook, customer, location
order           -> inventory, payment, customer, cart, tax, webhook
cart            -> product, promotion, tax
customer        -> location
payment         -> webhook, payment-paypal
delivery        -> order
recommendation  -> product, order
```

Flow nội bộ phát hiện thêm từ `application.properties`/`application.yaml` cần kiểm chứng:

```text
product         -> media, rating
cart            -> media, product
order           -> cart, customer, product, tax, promotion
payment         -> order, media
rating          -> product, customer, order
inventory       -> product, location
search          -> product
promotion       -> product
tax             -> location
recommendation  -> product, customer, order
storefront-bff  -> customer, cart, identity
webhook         -> webhook
```

Kết luận thực thi: viết allow rules tối thiểu theo plan trước để demo 200/403. Sau đó, nếu ứng dụng cần chạy flow end-to-end, bổ sung allow rules theo config thực tế và ghi lại trong README/báo cáo rằng policy được điều chỉnh sau khi quan sát traffic.

Cần tránh nhầm lẫn: `AuthorizationPolicy` được apply ở namespace của target workload. `selector.matchLabels` chọn service đích, `source.principals` là service gọi đến.

Ví dụ `storefront-bff -> product`:

```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: allow-storefront-bff-to-product
  namespace: dev
spec:
  selector:
    matchLabels:
      app: product
  action: ALLOW
  rules:
    - from:
        - source:
            principals:
              - "cluster.local/ns/dev/sa/storefront-bff"
```

---

## 4. Bảng Công Việc Hoàn Chỉnh Cho TV4

| Phase | Công việc | Đầu vào phụ thuộc | Đầu ra của TV4 | Tiêu chí hoàn thành |
|---|---|---|---|---|
| 0 | Chốt convention với TV1/TV3 | Tên namespace, service list, service port, label, ServiceAccount | Bảng convention TV4 dùng để viết YAML | Team xác nhận `app=<service>` và `sa=<service>` |
| 1 | Viết manifest Istio offline | Service map và convention | Thư mục `istio/` gồm mTLS, authz, retry, README | YAML validate được bằng `kubectl apply --dry-run=client` hoặc review syntax |
| 2 | Cài Istio và Kiali | TV1 cluster ready | Istio control plane, Kiali, Prometheus addon | `istiod`, `kiali`, `prometheus` Running |
| 3 | Bật sidecar injection | Namespace ready, app có thể restart | `dev`/`staging` có label injection, pods có sidecar | Pod trong `dev` hiện `2/2` containers |
| 4 | Apply và verify mTLS | Pods đã có sidecar | `PeerAuthentication`, `DestinationRule` | `istioctl x describe pod` thấy mTLS STRICT/TLS |
| 5 | Apply AuthorizationPolicy | TV3 ServiceAccount/labels đúng | Deny-all + allow rules theo service map | Allowed flow trả không 403, denied flow trả 403/RBAC denied |
| 6 | Apply retry policy | Endpoint test ổn định | `VirtualService` retry cho critical services | Test fault/5xx có evidence retry trong Kiali/metrics/log |
| 7 | Kiali topology | Có traffic thực tế | Screenshot graph, security, service detail, retry evidence | Báo cáo có hình topology và giải thích flow |
| 8 | Báo cáo tổng hợp | Evidence từ TV1/TV2/TV3/TV4 | File `.docx` theo format MSSV | Báo cáo có đủ screenshot, command logs, giải thích architecture |

---

## 5. Files TV4 Cần Tạo

Trong repo `yas`:

```text
istio/
├── README.md
├── mtls/
│   ├── peer-authentication-dev.yaml
│   ├── peer-authentication-staging.yaml
│   ├── destination-rule-dev.yaml
│   └── destination-rule-staging.yaml
├── authorization/
│   ├── allow/
│   │   ├── dev-allow-target-product.yaml
│   │   ├── dev-allow-target-media.yaml
│   │   ├── dev-allow-target-cart.yaml
│   │   ├── dev-allow-target-order.yaml
│   │   ├── dev-allow-target-customer.yaml
│   │   ├── dev-allow-target-rating.yaml
│   │   ├── dev-allow-target-search.yaml
│   │   ├── dev-allow-target-promotion.yaml
│   │   ├── dev-allow-target-tax.yaml
│   │   ├── dev-allow-target-location.yaml
│   │   ├── dev-allow-target-inventory.yaml
│   │   ├── dev-allow-target-payment.yaml
│   │   ├── dev-allow-target-webhook.yaml
│   │   ├── dev-allow-target-payment-paypal.yaml
│   │   └── ...
│   └── deny-final/
│       └── dev-deny-all.yaml
├── virtual-services/
│   ├── dev-product-vs.yaml
│   ├── dev-order-vs.yaml
│   ├── dev-payment-vs.yaml
│   ├── dev-cart-vs.yaml
│   └── dev-inventory-vs.yaml
└── tests/
    ├── curl-allowed-denied.md
    ├── retry-test.md
    └── kiali-screenshots.md
```

Nếu muốn áp dụng cho `staging`, tạo thêm bản staging sau khi `dev` chạy đúng. Cách đơn giản là duplicate file `dev-*`, đổi `namespace: staging` và principal từ `cluster.local/ns/dev/sa/...` sang `cluster.local/ns/staging/sa/...`.

---

## 6. Hướng Dẫn Thực Hiện Theo Phase

### Phase 0 — Chốt convention với TV3

Cần xác nhận trước khi viết policy:

```bash
kubectl get sa -n dev
kubectl get deploy -n dev -o jsonpath='{range .items[*]}{.metadata.name}{" -> sa="}{.spec.template.spec.serviceAccountName}{" labels="}{.spec.template.metadata.labels}{"\n"}{end}'
kubectl get svc -n dev
```

Expected:

- Mỗi service có ServiceAccount riêng: `product`, `cart`, `order`, ...
- Deployment dùng `serviceAccountName` khớp service.
- Pod label có `app=<service>`.
- Service DNS có dạng `<service>.dev.svc.cluster.local`.

### Phase 1 — Viết YAML offline

Cần tạo:

- mTLS:
  - `PeerAuthentication` cho `dev`, mode `STRICT`.
  - `DestinationRule` cho `*.dev.svc.cluster.local`, TLS `ISTIO_MUTUAL`.
- Authorization:
  - `dev-deny-all.yaml`: default deny trong namespace `dev`.
  - allow rules theo communication map.
- Retry:
  - `VirtualService` cho `product`, `order`, `payment`, `cart`, `inventory`.
  - Retry `attempts: 3`, `perTryTimeout: 2s`, `retryOn: 5xx,reset,connect-failure,retriable-4xx`.
- README:
  - Thứ tự apply.
  - Lệnh verify.
  - Cách rollback policy nếu app bị chặn nhầm.

### Phase 2 — Cài Istio + Kiali

Chạy trên máy có kubeconfig vào cluster:

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH="$PWD/bin:$PATH"
istioctl version
istioctl install --set profile=demo -y

kubectl get pods -n istio-system
```

Cài addon phục vụ Kiali:

```bash
kubectl apply -f samples/addons/prometheus.yaml
kubectl apply -f samples/addons/kiali.yaml
kubectl wait --for=condition=Ready pods --all -n istio-system --timeout=300s
```

Không bắt buộc cài Grafana/Loki/Tempo cho phần TV4. Kiali cần Prometheus để có graph/metrics.

### Phase 3 — Bật sidecar injection

Làm trước khi app deploy là tốt nhất:

```bash
kubectl label namespace dev istio-injection=enabled --overwrite
kubectl label namespace staging istio-injection=enabled --overwrite
kubectl get ns --show-labels
```

Nếu pod đã chạy trước đó, restart:

```bash
kubectl rollout restart deployment --all -n dev
kubectl rollout status deployment --all -n dev
kubectl get pods -n dev
```

Expected: app pod có `2/2` containers.

### Phase 4 — Apply và verify mTLS

```bash
kubectl apply -f istio/mtls/peer-authentication-dev.yaml
kubectl apply -f istio/mtls/destination-rule-dev.yaml

POD=$(kubectl get pods -n dev -l app=product -o jsonpath='{.items[0].metadata.name}')
istioctl x describe pod "$POD" -n dev
```

Evidence cần chụp:

- `kubectl get pods -n dev` hiện `2/2`.
- `istioctl x describe pod` hiện mTLS STRICT/TLS.
- Kiali security graph có padlock.

### Phase 5 — Apply AuthorizationPolicy

Chỉ thực hiện phase này ở cuối, sau khi mTLS, retry, Kiali và baseline traffic đã ổn. Lưu ý: chỉ cần apply `ALLOW` policy cho một workload là workload đó đã bắt đầu deny các request không match rule, vì vậy không apply `istio/authorization/allow/` sớm.

Cần apply theo thứ tự để chứng minh được yêu cầu:

1. Test baseline khi chưa có AuthorizationPolicy.
2. Apply allow policies.
3. Apply deny-all.
4. Test một kết nối được phép.
5. Test một kết nối không được phép bị chặn.

Lệnh mẫu:

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Expected baseline: không phải `403`.

Sau đó apply policy ở bước cuối:

```bash
kubectl apply -f istio/authorization/allow/
kubectl apply -f istio/authorization/deny-final/dev-deny-all.yaml

kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
  curl -s -o /dev/null -w "%{http_code}\n" \
  http://product.dev.svc.cluster.local:8080/product/actuator/health
```

Expected: không phải `403`. Tuy endpoint gốc có thể trả `404` nếu path sai; evidence tốt nhất là dùng endpoint có thật để ra `200`.

Denied test:

```bash
SEARCH=$(kubectl get pods -n dev -l app=search -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev "$SEARCH" -c search -- \
  curl -v http://payment.dev.svc.cluster.local:8081/
```

Expected: `403` / `RBAC: access denied`.

Rollback nhanh nếu policy làm app fail:

```bash
kubectl delete authorizationpolicy --all -n dev
```

### Phase 6 — Retry policy

Apply VirtualService:

```bash
kubectl apply -f istio/virtual-services/
```

Chọn một service để demo, nên là `product` vì dễ tạo traffic từ `storefront-bff`.

Có hai cách test:

| Cách | Ưu điểm | Nhược điểm |
|---|---|---|
| Fault injection bằng Istio | Không cần sửa code app | Cần viết temporary VirtualService riêng và chụp evidence cẩn thận |
| Tạo endpoint/app lỗi 500 thật | Evidence thuyết phục hơn | Cần TV2/dev hỗ trợ code hoặc config |

Khuyến nghị cho đồ án: dùng fault injection tạm thời, ghi rõ trong báo cáo đây là kịch bản test retry của service mesh.

Temporary fault example:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-fault-test
  namespace: dev
spec:
  hosts:
    - product
  http:
    - fault:
        abort:
          percentage:
            value: 50
          httpStatus: 500
      route:
        - destination:
            host: product
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx,reset,connect-failure,retriable-4xx
```

Sau khi test xong:

```bash
kubectl delete virtualservice product-fault-test -n dev
kubectl apply -f istio/virtual-services/dev-product-vs.yaml
```

Evidence cần có:

- Output nhiều request liên tiếp.
- Kiali metrics hoặc graph có retry/error/request rate.
- YAML retry policy trong report.

### Phase 7 — Kiali topology

Expose Kiali bằng port-forward hoặc NodePort.

```bash
istioctl dashboard kiali
```

Hoặc:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Generate traffic:

```bash
CLIENT=$(kubectl get pods -n dev -l app=storefront-bff -o jsonpath='{.items[0].metadata.name}')
while true; do
  kubectl exec -n dev "$CLIENT" -c storefront-bff -- \
    curl -s http://product.dev.svc.cluster.local:8080/ >/dev/null
  sleep 2
done
```

Screenshots cần chụp:

- Graph namespace `dev`, service graph.
- Security display có mTLS padlock.
- Traffic animation/request rate.
- Service detail của `product` hoặc `order`.
- Retry/error evidence sau fault test.

---

## 7. Checklist Báo Cáo Của TV4

### 7.1 Evidence riêng TV4

| Evidence | Lệnh/màn hình | Mục đích |
|---|---|---|
| Istio installed | `kubectl get pods -n istio-system` | Chứng minh Istio/Kiali running |
| Sidecar injected | `kubectl get pods -n dev` thấy `2/2` | Chứng minh workload vào mesh |
| mTLS strict | `istioctl x describe pod <pod> -n dev` | Chứng minh mTLS |
| Deny-all works | curl trước allow policy trả `403` | Chứng minh default deny |
| Allowed flow works | storefront-bff -> product không `403` | Chứng minh whitelist |
| Denied flow works | search -> payment trả `403` | Chứng minh service không được phép bị chặn |
| Retry works | VirtualService + fault test + Kiali metrics | Chứng minh retryable |
| Kiali topology | Graph screenshots | Chứng minh topology/flow |

### 7.2 Evidence cần xin từ TV1/TV2/TV3

| Thành viên | Cần xin |
|---|---|
| TV1 | VM, firewall, `kubectl get nodes`, namespace, ArgoCD, infra services, Jenkins agent |
| TV2 | Jenkins CI logs, Docker Hub image tags, developer_build run, cleanup job, GitOps update |
| TV3 | GitOps repo structure, Kustomize overlays, ServiceAccount manifests, ArgoCD sync result |

### 7.3 Cấu trúc mục Service Mesh trong báo cáo

```text
6. Service Mesh với Istio
   6.1 Mục tiêu và vị trí trong kiến trúc
   6.2 Cài đặt Istio và Kiali
   6.3 Bật sidecar injection cho namespace dev/staging
   6.4 mTLS STRICT: YAML + verify
   6.5 AuthorizationPolicy: deny-all + allowlist + test 200/403
   6.6 RetryPolicy: VirtualService + fault injection + evidence
   6.7 Kiali topology: graph + giải thích các flow chính
   6.8 Khó khăn/blocker và cách xử lý
```

---

## 8. Definition of Done Cho TV4

TV4 được xem là hoàn thành khi có đủ các mục sau:

- [ ] Có thư mục `istio/` với YAML mTLS, AuthorizationPolicy, VirtualService và README.
- [ ] Istio control plane và Kiali chạy được trên cluster.
- [ ] Namespace `dev` có sidecar injection, app pods hiện `2/2`.
- [ ] mTLS STRICT được verify bằng `istioctl`.
- [ ] Có ít nhất 1 allowed test và 1 denied test bằng curl.
- [ ] Có retry policy và retry/fault-test evidence.
- [ ] Có Kiali topology screenshots.
- [ ] Có nội dung báo cáo Service Mesh hoàn chỉnh.
- [ ] Đã tổng hợp screenshot/logs từ TV1, TV2, TV3 cho file `.docx`.

---

## 9. Rủi Ro Và Cách Xử Lý

| Rủi ro | Dấu hiệu | Cách xử lý |
|---|---|---|
| Pod không có sidecar | `kubectl get pods` chỉ `1/1` | Label namespace và rollout restart |
| Deny-all làm app fail toàn bộ | Nhiều request trả 403 | Tạm xóa `AuthorizationPolicy`, apply allow rules lại từng bước |
| Policy không match | Allowed flow vẫn 403 | Kiểm tra `serviceAccountName`, principal, pod label `app` |
| Kiali không hiện graph | Graph empty | Cài Prometheus addon, generate traffic 5-10 phút |
| Retry không thấy rõ | Request vẫn lỗi nhiều | Kiểm tra VirtualService host, route, fault config, dùng Kiali metrics |
| Endpoint curl sai | Trả 404 thay vì 200 | Phân biệt 404 app-level với 403 RBAC; xin TV2/TV3 endpoint đúng |
| Resource VM thiếu | Istio/Kiali/app pod Pending/CrashLoop | Giảm replica, tắt addon không cần, thông báo TV1 cần thêm RAM/CPU |
