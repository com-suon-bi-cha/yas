# Project 2 - Khung Báo Cáo Theo Thành Viên

File này là mục lục cho các khung báo cáo cá nhân. Mỗi thành viên điền nội dung và screenshot vào file riêng, sau đó nhóm dùng các file này làm nguồn để viết lại báo cáo hoàn chỉnh bằng LaTeX.

## Quy Ước Chung

- Viết bằng tiếng Việt có dấu.
- Mỗi screenshot phải có:
  - đường dẫn ảnh;
  - caption dự kiến;
  - 1-3 câu giải thích ảnh chứng minh điều gì.
- Không ghi service ngoài scope như workload đã deploy nếu thực tế không deploy.
- Scope hiện tại:
  - 16 workload ứng dụng: `product`, `cart`, `order`, `customer`, `inventory`, `tax`, `payment`, `media`, `search`, `location`, `storefront-bff`, `storefront-ui`, `backoffice-bff`, `backoffice-ui`, `swagger-ui`, `sampledata`.
  - Workload hỗ trợ: `kafka-connect`.

## File Theo Thành Viên

| Thành viên | File | Nội dung chính |
|------------|------|----------------|
| TV1 | [member1-report.md](member1-report.md) | GCP, K3s, infra, ArgoCD, Jenkins agent |
| TV2 | [member2-report.md](member2-report.md) | Jenkins CI/CD, Docker Hub, GitOps update, developer-build |
| TV3 | [member3-report.md](member3-report.md) | GitOps repo, Kustomize, dev/staging/developer-build |
| TV4 | [member4-report.md](member4-report.md) | Istio, mTLS, AuthorizationPolicy, retry, Kiali |
| Nhóm | [observability-report.md](observability-report.md) | Observability stack: Prometheus, Grafana, Loki, Tempo, OpenTelemetry |

## Thư Mục Ảnh Khuyến Nghị

```text
docs/images/member1-report/
docs/images/member2-report/
docs/images/member3-report/
docs/images/member4-report/
docs/images/observability-report/
```

Tên ảnh nên dùng số thứ tự:

```text
01-gcp-vm-running.png
02-kubectl-nodes.png
03-argocd-yas-dev-synced.png
```

Khi chuyển sang LaTeX, copy ảnh vào thư mục `img/` của report LaTeX và giữ caption tương ứng.
