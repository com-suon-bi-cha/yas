# TV3 - GitOps Manifests Với Kustomize

## Vai Trò

TV3 chịu trách nhiệm repo `gitops-manifest-k8s`:

- Tạo và bảo trì Kustomize base/overlays.
- Quản lý Deployment, Service, ServiceAccount cho workload ứng dụng.
- Quản lý `dev`, `staging`, `developer-build`.
- Đảm bảo ArgoCD render đúng desired state.
- Phối hợp TV2 để update image tags.
- Phối hợp TV4 để bảo đảm labels/ServiceAccount đúng cho Istio AuthorizationPolicy.

## Scope Hiện Tại

### `dev` và `staging`

Hai môi trường này render 17 Deployment:

- 16 workload ứng dụng:
  `product`, `cart`, `order`, `customer`, `inventory`, `tax`, `payment`, `media`, `search`, `location`, `storefront-bff`, `storefront-ui`, `backoffice-bff`, `backoffice-ui`, `swagger-ui`, `sampledata`.
- 1 workload hỗ trợ:
  `kafka-connect`.

### `developer-build`

- Render 16 workload ứng dụng.
- Có NodePort Service.
- Deployment đang scale `0/0` khi không có job test.
- Dùng ExternalName/patch để gọi infra ở `dev`.

### Ngoài scope deploy hiện tại

Không render thành Deployment trong `dev`/`staging`:

```text
promotion
rating
delivery
recommendation
webhook
payment-paypal
```

## Trạng Thái Hiện Tại

- [x] Repo `gitops-manifest-k8s` branch `main` sạch và đồng bộ `origin/main`.
- [x] `base` chứa 15 workload ban đầu, ingress Istio và common identity.
- [x] `base/location` được include thêm trong overlays.
- [x] `base/kafka-connect` được include trong `dev` và `staging`.
- [x] `environments/dev` render 17 Deployment.
- [x] `environments/staging` render 17 Deployment.
- [x] `environments/developer-build` render 16 Deployment và NodePort Service.
- [x] ArgoCD `yas-dev` và `yas-staging` đã kiểm tra `Synced/Healthy`.
- [ ] Kiểm tra lại ArgoCD ngay trước khi chụp báo cáo vì Jenkins có thể update image tag mới.

## Checklist Công Việc

### 1. Cấu Trúc Repo

- [ ] Chụp cây thư mục chính:

```bash
tree -L 3 gitops-manifest-k8s
```

Hoặc:

```bash
find base environments -maxdepth 3 -type f | sort
```

### 2. Validate Render

```bash
kubectl kustomize environments/dev > /tmp/yas-dev-rendered.yaml
kubectl kustomize environments/staging > /tmp/yas-staging-rendered.yaml
kubectl kustomize environments/developer-build > /tmp/yas-developer-build-rendered.yaml
```

Kiểm tra Deployment:

```bash
awk '/^kind: Deployment$/{in_dep=1; next} in_dep && /^metadata:/{next} in_dep && /^  name: /{sub(/^  name: /,""); print; in_dep=0}' /tmp/yas-dev-rendered.yaml | sort
```

Kiểm tra service ngoài scope không render:

```bash
grep -nE 'name: (promotion|rating|delivery|recommendation|webhook|payment-paypal)$' /tmp/yas-dev-rendered.yaml || true
```

### 3. Dev Overlay

- [ ] Xác nhận `environments/dev/kustomization.yaml` include:
  - `../../base`
  - `../../base/location`
  - `../../base/kafka-connect`
  - `patches/kafka-connect-connector.yaml`
  - `istio/mtls.yaml`
  - `istio/retry.yaml`
  - `istio/authorization.yaml`
- [ ] Chụp image tags hiện tại:

```bash
sed -n '/^images:/,$p' environments/dev/kustomization.yaml
```

### 4. Staging Overlay

- [ ] Xác nhận `staging` render cùng scope workload với `dev`.
- [ ] Ghi rõ staging hiện có ingress Istio nhưng chưa bật đầy đủ mTLS/Authz/retry như `dev`.
- [ ] Chụp ArgoCD `yas-staging` `Synced/Healthy`.
- [ ] Chụp ArgoCD `yas-dev` sau khi rollout mới hoàn tất.

### 5. Developer-Build Overlay

- [ ] Xác nhận NodePort services:

```bash
kubectl kustomize environments/developer-build | grep -n 'type: NodePort'
kubectl get svc -n developer-build
```

- [ ] Ghi rõ Deployment hiện scale `0/0` để tiết kiệm tài nguyên.
- [ ] Nếu thấy ServiceAccount cũ như `delivery`, `promotion`, `webhook` còn trong namespace live, ghi là leftover resource, không phải workload đang chạy.

### 6. ArgoCD

```bash
kubectl get application yas-dev yas-staging -n argocd -o wide
```

- [ ] Chụp UI ArgoCD resource tree.
- [ ] Chụp Git commit history của `gitops-manifest-k8s`.

## Deliverables Cho Báo Cáo

- Mô tả GitOps repo structure.
- Mô tả Kustomize base/overlay.
- Bảng scope dev/staging/developer-build.
- Bằng chứng render và ArgoCD sync.
- Screenshot minh chứng theo [member3-report.md](member3-report.md).
