# TV2 - CI/CD Pipelines Với Jenkins

## Vai Trò

TV2 chịu trách nhiệm pipeline CI/CD:

- Jenkins Multibranch Pipeline cho repo `yas`.
- Build/test theo monorepo.
- Docker build/push lên Docker Hub `bingsu1103`.
- Update image tag trong repo `gitops-manifest-k8s`.
- Job `developer_build`.
- Job cleanup cho `developer-build`.
- Cung cấp bằng chứng pipeline cho báo cáo.

## Scope Build/Deploy Hiện Tại

### Backend/app services trong scope

```text
media
product
order
inventory
payment
sampledata
customer
location
cart
tax
search
backoffice-bff
storefront-bff
backoffice
storefront
```

Ghi chú:

- `backoffice` là source/image của `backoffice-ui`.
- `storefront` là source/image của `storefront-ui`.
- `swagger-ui` dùng image public, không build từ source YAS.
- `kafka-connect` dùng image public `quay.io/debezium/connect:2.4`, không build trong Jenkins YAS.

### Ngoài scope pipeline hiện tại

```text
promotion
rating
delivery
recommendation
webhook
payment-paypal
```

Không đưa các service này vào báo cáo như service đang deploy, trừ khi ghi rõ là ngoài scope hiện tại.

## Trạng Thái Hiện Tại

- [x] `Jenkinsfile.ci` có danh sách service theo scope hiện tại, đã có `location`.
- [x] `Jenkinsfile.developer-build` có parameter cho 16 workload ứng dụng.
- [x] `scripts/update-gitops-manifest.sh` update image tag cho `dev` và `staging`.
- [x] `developer-build` có NodePort Service cho 16 workload.
- [x] ArgoCD `yas-dev` và `yas-staging` đã kiểm tra `Synced/Healthy`.
- [ ] Kiểm tra lại ArgoCD sau mỗi GitOps commit mới trước khi chụp báo cáo.
- [ ] Cần chụp Jenkins run mới nhất để đưa vào báo cáo.

## Checklist Công Việc

### 1. Jenkins Credentials

- [ ] Verify các credentials:

| Credential ID | Loại | Dùng cho |
|---------------|------|----------|
| `dockerhub-cred` | username/password | Docker Hub push |
| `gcp-kubeconfig` | secret file | Deploy/check cluster |
| `github-pat` hoặc `github-token` | token | Push GitOps repo |
| `sonar-token` | secret text | SonarQube nếu dùng |
| `snyk-token` | secret text | Snyk nếu dùng |

### 2. Jenkinsfile.ci

- [ ] Chụp Jenkins job config: script path `Jenkinsfile.ci`.
- [ ] Chạy pipeline trên branch/main phù hợp.
- [ ] Chụp các stage chính:
  - Pre-check
  - Secret Scanning
  - Monorepo Execution
  - Code Quality/Quality Gate nếu có
  - Coverage Report nếu có
  - Dependency Scan
  - Docker Build & Push
  - Update GitOps Dev/Staging

Kiểm tra scope bằng lệnh:

```bash
grep -nE "backendServices|dockerServices|location|promotion|delivery|payment-paypal" Jenkinsfile.ci
```

### 3. Docker Build & Push

- [ ] Chứng minh image được push:

```bash
docker manifest inspect bingsu1103/product:latest
docker manifest inspect bingsu1103/location:latest
docker manifest inspect bingsu1103/storefront:latest
docker manifest inspect bingsu1103/backoffice:latest
```

- [ ] Chụp Docker Hub tags cho ít nhất:
  - `product`
  - `location`
  - `storefront`
  - `backoffice`

### 4. Update GitOps

- [ ] Chụp commit Jenkins tạo trong `gitops-manifest-k8s`.
- [ ] Chứng minh `environments/dev/kustomization.yaml` đổi tag đúng.
- [ ] Chứng minh ArgoCD sync sau commit; nếu app đang `Progressing`, chờ pod ready rồi chụp:

```bash
kubectl get application yas-dev yas-staging -n argocd -o wide
```

### 5. Developer Build

- [ ] Chạy job `developer_build` với một service mẫu, ví dụ `tax` hoặc `location`.
- [ ] Chụp parameter page.
- [ ] Chụp console output bảng NodePort.
- [ ] Verify service sau deploy:

```bash
kubectl get deploy,svc -n developer-build
curl http://<WORKER_IP>:<NODE_PORT>/<service>/actuator/health
```

Ghi chú trạng thái hiện tại: `developer-build` đang có NodePort Service nhưng Deployment scale `0/0` khi chưa chạy job.

### 6. Cleanup

- [ ] Chạy cleanup job nếu nhóm demo developer-build.
- [ ] Chụp trước/sau:

```bash
kubectl get deploy,svc -n developer-build
```

## Deliverables Cho Báo Cáo

- Mô tả Jenkins pipeline.
- Mô tả logic monorepo service detection.
- Mô tả Docker tag convention.
- Mô tả GitOps update flow.
- Mô tả developer-build và cleanup.
- Screenshot minh chứng theo [member2-report.md](member2-report.md).
