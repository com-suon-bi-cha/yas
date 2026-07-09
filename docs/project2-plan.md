# Đồ Án 2 - Kế Hoạch Tổng Thể CI/CD, GitOps Và Service Mesh

Tài liệu này mô tả kiến trúc và phân công hiện tại của Project 2. Chi tiết trạng thái live nằm ở [project2-cicd-scope-plan.md](project2-cicd-scope-plan.md).

## 1. Kiến Trúc Tổng Quan

```text
Developer push code
        |
        v
GitHub webhook
        |
        v
Jenkins multibranch pipeline
        |
        +-- test/build selected services
        +-- docker build/push -> Docker Hub bingsu1103
        +-- update gitops-manifest-k8s
                         |
                         v
                    ArgoCD watches
                         |
             +-----------+------------+
             v                        v
        namespace dev           namespace staging
             |
             v
      K3s cluster on GCP VM
      + infra: Postgres, Kafka, Keycloak, Redis, Elasticsearch
      + GitOps workloads
      + Istio/Kiali/Prometheus/Grafana
```

## 2. Scope Hiện Tại

### Workload ứng dụng

`dev` và `staging` hiện deploy 16 workload ứng dụng:

```text
product
cart
order
customer
inventory
tax
payment
media
search
location
storefront-bff
storefront-ui
backoffice-bff
backoffice-ui
swagger-ui
sampledata
```

### Workload hỗ trợ

```text
kafka-connect
```

`kafka-connect` phục vụ Debezium/CDC, không tính là workload ứng dụng chính.

### Ngoài scope deploy hiện tại

```text
promotion
rating
delivery
recommendation
webhook
payment-paypal
```

Các service này chỉ được đưa vào nếu có kịch bản demo riêng và phải cập nhật đồng bộ CI, GitOps, Istio, báo cáo.

## 3. Environment

| Environment | Trạng thái | Scope | Ghi chú |
|-------------|------------|-------|---------|
| `dev` | `Synced/Healthy` khi kiểm tra gần nhất | 16 app workload + `kafka-connect` | Có mTLS, retry, AuthorizationPolicy; kiểm tra lại nếu Jenkins vừa tạo GitOps commit mới |
| `staging` | `Synced/Healthy` khi kiểm tra gần nhất | 16 app workload + `kafka-connect` | Có Gateway/VirtualService ingress, chưa bật đầy đủ Service Mesh policy |
| `developer-build` | Deployment `0/0`, NodePort Service tồn tại | 16 app workload | Job Jenkins sẽ scale/deploy khi test branch |

## 4. Công Nghệ Sử Dụng

| Nhóm | Công nghệ |
|------|-----------|
| CI | Jenkins Multibranch Pipeline |
| Registry | Docker Hub `bingsu1103/*` |
| CD | ArgoCD |
| Manifest | Kustomize trong repo `gitops-manifest-k8s` |
| Cluster | K3s trên GCP VM |
| Service Mesh | Istio |
| Observability | Kiali, Prometheus, Grafana, Loki, Tempo, Promtail, OpenTelemetry Collector |
| Auth | Keycloak + BFF pattern |
| Data/CDC | PostgreSQL, Kafka, Debezium/Kafka Connect, Elasticsearch |

## 5. Phân Công Thành Viên

| Thành viên | Phạm vi chính | Tài liệu chi tiết | Report skeleton |
|------------|---------------|-------------------|-----------------|
| TV1 | GCP VM, K3s, namespaces, infra services, ArgoCD, Jenkins agent | [member1-tasks.md](member1-tasks.md) | [member1-report.md](member1-report.md) |
| TV2 | Jenkins CI/CD, Docker build/push, developer-build, cleanup, GitOps update script | [member2-tasks.md](member2-tasks.md) | [member2-report.md](member2-report.md) |
| TV3 | GitOps manifests, Kustomize overlays, dev/staging/developer-build, ArgoCD app resources | [member3-tasks.md](member3-tasks.md) | [member3-report.md](member3-report.md) |
| TV4 | Istio Service Mesh, mTLS, AuthorizationPolicy, retry, Kiali, tổng hợp bằng chứng | [member4-tasks.md](member4-tasks.md) | [member4-report.md](member4-report.md) |
| Nhóm | Observability stack: Prometheus/Grafana metrics, Loki logs, Tempo traces, OpenTelemetry Collector | - | [observability-report.md](observability-report.md) |

## 6. Dependency Giữa Các Thành Viên

```text
TV1 dựng cluster + infra + ArgoCD
    -> TV3 có nơi sync GitOps
    -> TV2 có kubeconfig/Jenkins agent để deploy
    -> TV4 có Istio/Kiali và app pods để kiểm thử mesh

TV3 tạo GitOps manifests
    -> TV2 update image tag trong overlay
    -> TV4 dựa vào ServiceAccount/app labels để viết AuthorizationPolicy

TV2 build/push image và update GitOps
    -> ArgoCD rollout dev/staging
    -> TV4 có traffic thật để chụp Kiali

TV4 bật mesh/policy và kiểm chứng security
    -> nhóm có bằng chứng NC2 và nội dung báo cáo Service Mesh
```

## 7. Kịch Bản Demo Tối Thiểu

1. Chứng minh ArgoCD `yas-dev` và `yas-staging` ở trạng thái ổn định tại thời điểm demo; nếu `yas-dev` đang rollout, chờ pod ready rồi chụp lại.
2. Chứng minh `dev` có 16 app workload + `kafka-connect` Running.
3. Truy cập storefront qua Istio ingress.
4. Truy cập swagger qua Istio ingress.
5. Gọi API product categories qua BFF.
6. Gọi API payment providers qua BFF.
7. Chứng minh Service Mesh:
   - pod có sidecar `2/2`;
   - `PeerAuthentication STRICT`;
   - `DestinationRule ISTIO_MUTUAL`;
   - retry policy;
   - allowed curl `200`;
   - denied curl `403 RBAC`;
   - Kiali graph/security.

## 8. Checklist Trước Khi Viết Báo Cáo LaTeX

- [ ] Các task docs của 4 thành viên khớp scope hiện tại.
- [ ] Mỗi thành viên điền report skeleton của mình.
- [ ] Screenshot được đặt trong thư mục thống nhất, ví dụ `docs/images/member1-report/`.
- [ ] Mỗi ảnh có caption tiếng Việt và mô tả ngắn ý nghĩa.
- [ ] Không đưa service ngoài scope vào báo cáo như workload đã deploy, trừ khi nói rõ là ngoài scope hoặc tài nguyên cũ.
