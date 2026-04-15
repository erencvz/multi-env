# YOUR_REPO_NAME

Multi-environment Kubernetes deployment sürecini simüle eden demo projesi. FastAPI tabanlı bir mesajlaşma API'si, GitHub Actions ile CI, Jenkins ile CD (normal akış) ve Argo CD ile hedefli deploy (targeted deploy) senaryolarını içerir. API Gateway katmanında Apinizer kullanılmaktadır.

---

## Mimari

```
GitHub Actions (CI)
  └─► Docker Hub'a image push
        └─► Jenkins'i tetikle (VERSION_TAG ile)

Jenkins (CD — Normal Akış)
  └─► Dev deploy → Apinizer Sync → Smoke Test
        └─► [Onay] Test deploy → Apinizer Sync → Smoke Test
              └─► [Onay] UAT deploy → Apinizer Sync → Smoke Test
                    └─► [Onay] Prod deploy → Apinizer Sync → Smoke Test

Argo CD (CD — Targeted Deploy)
  └─► Overlay'deki newTag güncellenir → Argo CD sync edilir
```

---

## Ortamlar

| Ortam | Namespace              | NodePort | URL                              |
|-------|------------------------|----------|----------------------------------|
| dev   | multi-env-dev      | 30221    | http://167.86.118.166:30221      |
| test  | multi-env-test     | 30222    | http://167.86.118.166:30222      |
| uat   | multi-env-uat      | 30223    | http://167.86.118.166:30223      |
| prod  | multi-env-prod     | 30224    | http://167.86.118.166:30224      |

---

## Pipeline 1 — Normal Akış (GitHub Actions + Jenkins)

1. `main` branch'e push yapılır.
2. GitHub Actions testleri çalıştırır, semantic version tag üretir (örn. `v1.2.3`), Docker image'ı build edip Docker Hub'a pushlar (hem `v1.2.3` hem `latest` tag ile).
3. GitHub Actions Jenkins pipeline'ını `VERSION_TAG=v1.2.3` parametresiyle tetikler.
4. Jenkins sırasıyla dev → test → uat → prod ortamlarına deploy eder. Her ortamda Kustomize ile image tag güncellenir, `kubectl apply -k` çalışır, Apinizer proxy sync edilir ve smoke test yapılır. Test, UAT ve prod geçişleri için manuel onay beklenir.

### Shared Library

`jenkins/shared-library/vars/` altındaki `.groovy` dosyaları Jenkins'e Global Pipeline Library olarak tanıtılır. Jenkinsfile'ın kendisi yalnızca stage tanımlarını içerir; tüm helper logic ayrı dosyalardadır.

---

## Pipeline 2 — Targeted Deploy (Argo CD)

Belirli bir ortama belirli bir versiyon deploy etmek için:

1. İlgili overlay'deki `kustomization.yaml` dosyasında `newTag` değeri istenen versiyon ile güncellenir ve commit + push yapılır.
2. Argo CD UI'dan ilgili Application seçilir ve **Sync** butonuna basılır.
3. Argo CD cluster'ı güncel state ile reconcile eder; pod'lar yeni image ile ayağa kalkar.

Argo CD Application manifest'leri `k8s/argocd/` altındadır. `syncPolicy` kasıtlı olarak **manual** bırakılmıştır; otomatik sync istenmez çünkü hangi versiyon, hangi ortama, ne zaman gideceği kontrollü tutulacaktır.

---

## Gerekli Jenkins Credentials

| Credential ID            | Tür         | Açıklama                                  |
|--------------------------|-------------|-------------------------------------------|
| `kubeconfig`             | Secret File | Kubernetes cluster erişim konfigürasyonu  |
| `apinizer-management-url`| Secret Text | Apinizer Manager URL (örn. https://...)   |
| `APINIZER_DEMO_TOKEN`    | Secret Text | Apinizer Bearer token                     |

---

## Gerekli GitHub Secrets

| Secret Adı          | Açıklama                                         |
|---------------------|--------------------------------------------------|
| `DOCKERHUB_USERNAME`| Docker Hub kullanıcı adı                         |
| `DOCKERHUB_TOKEN`   | Docker Hub access token                          |
| `JENKINS_URL`       | Jenkins sunucu URL (örn. http://jenkins:8080)    |
| `JENKINS_JOB_NAME`  | Jenkins job adı (boşluk varsa encode edilmeden yazılır, CI halleder) |
| `JENKINS_USER`      | Jenkins kullanıcı adı                            |
| `JENKINS_TOKEN`     | Jenkins API token                                |
