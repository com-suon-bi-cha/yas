# Báo Cáo TV1 - Hạ Tầng GCP, K3s, ArgoCD Và Infrastructure

## 1. Tóm Tắt Phần Việc

TV1 phụ trách dựng nền tảng hạ tầng để các thành viên khác triển khai CI/CD và Service Mesh. Nội dung gồm GCP VM, K3s cluster, namespaces, secrets, ArgoCD, infrastructure services và Jenkins agent/kubeconfig.

Viết 1 đoạn 5-8 câu trả lời các ý:

- Vì sao chọn mô hình GCP VM all-in-one.
- Cluster phục vụ những namespace nào.
- Infra service nào được triển khai.
- ArgoCD quản lý GitOps như thế nào.
- TV1 cung cấp gì cho TV2/TV3/TV4.

## 2. Kiến Trúc Hạ Tầng

Mô tả:

- GCP VM chạy K3s.
- `dev`, `staging`, `developer-build`, `argocd`, `istio-system`.
- `dev` và `staging` chạy 16 workload ứng dụng + `kafka-connect`.
- Infra dùng chung: PostgreSQL, Kafka, Keycloak, Redis, Elasticsearch.

Screenshot cần chèn:

```markdown
![GCP VM Running](images/member1-report/01-gcp-vm-running.png)
Caption: GCP VM all-in-one đang chạy để host K3s cluster và các dịch vụ DevOps.
```

```markdown
![Firewall Rules](images/member1-report/02-firewall-rules.png)
Caption: Firewall rules mở các cổng cần thiết cho Kubernetes API, ArgoCD, NodePort và Kiali.
```

## 3. K3s Cluster

Lệnh kiểm chứng:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl get ns
```

Screenshot cần chèn:

```markdown
![K3s Nodes](images/member1-report/03-k3s-nodes.png)
Caption: Node K3s ở trạng thái Ready.
```

```markdown
![Namespaces](images/member1-report/04-namespaces.png)
Caption: Các namespace chính của đồ án gồm dev, staging, developer-build, argocd và istio-system.
```

Nội dung viết:

- Giải thích `dev` dùng cho CD tự động.
- Giải thích `staging` dùng cho release/staging validation.
- Giải thích `developer-build` dùng để test branch/service riêng.

## 4. Secrets Và Kubeconfig

Lệnh kiểm chứng:

```bash
kubectl get secret dockerhub-secret -n dev
kubectl get secret dockerhub-secret -n staging
kubectl get secret dockerhub-secret -n developer-build
```

Screenshot cần chèn:

```markdown
![Docker Hub Pull Secret](images/member1-report/05-dockerhub-secret.png)
Caption: Docker Hub pull secret được tạo trong các namespace deploy.
```

Nội dung viết:

- Docker Hub secret giúp cluster pull image private/public dưới account nhóm.
- Kubeconfig external được cấu hình thành Jenkins credential, không commit vào Git.

## 5. ArgoCD

Lệnh kiểm chứng:

```bash
kubectl get pods -n argocd
kubectl get application yas-dev yas-staging -n argocd -o wide
```

Screenshot cần chèn:

```markdown
![ArgoCD Pods](images/member1-report/06-argocd-pods.png)
Caption: Các pod ArgoCD đang chạy trong namespace argocd.
```

```markdown
![ArgoCD Applications](images/member1-report/07-argocd-applications.png)
Caption: ArgoCD quản lý hai application yas-dev và yas-staging; ảnh cần chụp khi các rollout đã ổn định.
```

```markdown
![ArgoCD Resource Tree](images/member1-report/08-argocd-resource-tree.png)
Caption: Resource tree của yas-dev cho thấy workload ứng dụng và GitOps resources được ArgoCD quản lý.
```

## 6. Infrastructure Services

Lệnh kiểm chứng:

```bash
kubectl get pods,svc -n dev | grep -E 'postgres|kafka|keycloak|redis|elasticsearch|kafka-connect'
kubectl get pods,svc -n staging | grep -E 'postgres|kafka|keycloak|redis|elasticsearch|kafka-connect'
```

Screenshot cần chèn:

```markdown
![Dev Infrastructure](images/member1-report/09-dev-infra-services.png)
Caption: Các dịch vụ hạ tầng trong namespace dev đang Running.
```

```markdown
![Staging Infrastructure](images/member1-report/10-staging-infra-services.png)
Caption: Các dịch vụ hạ tầng trong namespace staging đang Running.
```

Nội dung viết:

- PostgreSQL lưu dữ liệu từng service.
- Kafka và `kafka-connect` phục vụ CDC/search synchronization.
- Keycloak cung cấp identity/authentication.
- Redis/Elasticsearch hỗ trợ cache/search.

## 7. Hỗ Trợ Các Thành Viên Khác

Viết ngắn:

- TV2 dùng kubeconfig và Jenkins agent để deploy/update GitOps.
- TV3 dùng ArgoCD và GitOps repo để sync manifest.
- TV4 dùng Istio/Kiali và app pods có sidecar để làm Service Mesh.

## 8. Kết Luận

Nêu rõ hạ tầng đã đáp ứng:

- Cluster sẵn sàng.
- ArgoCD quản lý được `yas-dev` và `yas-staging`; cần chụp trạng thái ổn định tại thời điểm demo.
- Infra service Running.
- Có nền tảng để CI/CD và Service Mesh hoạt động.
