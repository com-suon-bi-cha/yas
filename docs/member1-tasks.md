# TV1 - Hạ Tầng GCP, K3s, ArgoCD Và Infrastructure

## Vai Trò

TV1 chịu trách nhiệm dựng nền tảng chạy hệ thống:

- GCP VM all-in-one.
- K3s cluster.
- Namespace chuẩn: `dev`, `staging`, `developer-build`, `argocd`, `istio-system`.
- Docker Hub pull secret.
- ArgoCD.
- Infrastructure services: PostgreSQL, Kafka, Keycloak, Redis, Elasticsearch.
- Jenkins agent/kubeconfig để TV2 chạy pipeline.
- Phối hợp TV4 cài Istio/Kiali/Prometheus/Grafana nếu phần này được đặt ở hạ tầng.

## Scope Hiện Tại Cần Hỗ Trợ

TV1 không trực tiếp viết manifest app chính, nhưng phải đảm bảo cluster đủ tài nguyên và infra chạy ổn cho scope hiện tại:

- 16 workload ứng dụng YAS:
  `product`, `cart`, `order`, `customer`, `inventory`, `tax`, `payment`, `media`, `search`, `location`, `storefront-bff`, `storefront-ui`, `backoffice-bff`, `backoffice-ui`, `swagger-ui`, `sampledata`.
- 1 workload hỗ trợ: `kafka-connect`.
- Infra trong `dev` và `staging`: PostgreSQL, Kafka, Keycloak/identity, Redis, Elasticsearch.

## Trạng Thái Hiện Tại

- [x] Cluster đang chạy.
- [x] ArgoCD app `yas-dev` đã kiểm tra `Synced/Healthy`.
- [x] ArgoCD app `yas-staging` đã kiểm tra `Synced/Healthy`.
- [ ] Kiểm tra lại ngay trước khi chụp báo cáo vì Jenkins có thể vừa đẩy GitOps commit mới.
- [x] Namespace `dev` có workload app và infra Running.
- [x] Namespace `staging` có workload app và infra Running.
- [x] Namespace `dev`, `staging`, `developer-build` có Istio sidecar injection.
- [x] Istio system, Kiali, Prometheus, Grafana đang Running.
- [ ] Cần chụp lại đầy đủ screenshot cho báo cáo cuối.

## Checklist Công Việc

### 1. GCP VM Và Tooling

- [ ] Chụp VM Running trên GCP Console.
- [ ] Chụp firewall rules cho:
  - Kubernetes API `6443`
  - HTTP/HTTPS hoặc NodePort ingress
  - NodePort range `30000-32767`
  - Jenkins agent nếu dùng inbound agent
  - ArgoCD UI
  - Kiali UI nếu expose bằng NodePort
- [ ] Chụp output:

```bash
docker version
kubectl version --client
helm version
java --version
mvn --version
kustomize version
```

### 2. K3s Và Namespace

- [ ] Verify node:

```bash
kubectl get nodes -o wide
kubectl get pods -A
```

- [ ] Verify namespaces:

```bash
kubectl get ns
kubectl get ns dev staging developer-build --show-labels
```

### 3. Secrets Và Kubeconfig

- [ ] Verify Docker Hub pull secret:

```bash
kubectl get secret dockerhub-secret -n dev
kubectl get secret dockerhub-secret -n staging
kubectl get secret dockerhub-secret -n developer-build
```

- [ ] Chuẩn bị kubeconfig cho Jenkins credential `gcp-kubeconfig`.
- [ ] Không commit kubeconfig/token vào Git.

### 4. ArgoCD

- [ ] Verify ArgoCD pods:

```bash
kubectl get pods -n argocd
kubectl get svc -n argocd
```

- [ ] Verify applications:

```bash
kubectl get application yas-dev yas-staging -n argocd -o wide
```

- [ ] Chụp ArgoCD UI:
  - Repo connected.
  - App `yas-dev` ở trạng thái ổn định sau rollout.
  - App `yas-staging` Synced/Healthy.
  - Resource tree của `yas-dev`.

### 5. Infrastructure Services

- [ ] Verify infra trong `dev`:

```bash
kubectl get pods,svc -n dev | grep -E 'postgres|kafka|keycloak|redis|elasticsearch|kafka-connect'
```

- [ ] Verify infra trong `staging`:

```bash
kubectl get pods,svc -n staging | grep -E 'postgres|kafka|keycloak|redis|elasticsearch|kafka-connect'
```

- [ ] Ghi chú rõ `kafka-connect` là workload hỗ trợ CDC/GitOps hook, không phải app service chính.

### 6. Hỗ Trợ TV2/TV3/TV4

- [ ] Gửi Jenkins agent label và kubeconfig credential cho TV2.
- [ ] Xác nhận ArgoCD watch repo `gitops-manifest-k8s` cho TV3.
- [ ] Xác nhận Istio/Kiali sẵn sàng cho TV4:

```bash
kubectl get pods -n istio-system
kubectl get svc -n istio-system
```

## Deliverables Cho Báo Cáo

- Mô tả kiến trúc GCP VM all-in-one.
- Mô tả K3s namespaces.
- Mô tả ArgoCD quản lý GitOps.
- Mô tả infra services phục vụ YAS.
- Screenshot minh chứng theo [member1-report.md](member1-report.md).
