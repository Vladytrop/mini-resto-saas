Resto SaaS — Deployment & Testing Guide
📁 Where to put files
mini-resto-saas/
├── .github/workflows/build.yml          ← build-improved.yml (rename to build.yml)
├── services/api-gateway/
│   └── src/main/resources/
│       └── application.yml              ← application-gateway-ratelimit.yml
├── infra/
│   ├── k8s/
│   │   └── hpa.yml                      ← already exists ✅
│   ├── grafana/
│   │   └── provisioning/
│   │       ├── dashboards/
│   │       │   ├── dashboard.json     ← already exists ✅
│   │       │   └── dashboard.yml      ← already exists ✅
│   │       └── datasources/
│   │           └── prometheus.yml       ← already exists ✅
│   └── k6/
│       └── load-test-3000rps.js         ← new file
└── scripts/
└── cache-test.sh                    ← new file (chmod +x)
🚀 Quick Start (local with Kind)
# 1. Build all services
./gradlew clean bootJar

# 2. Build Docker images
docker build -t vladytrop/resto-order-service:latest ./services/order-service
docker build -t vladytrop/resto-loyalty-service:latest ./services/loyalty-service
docker build -t vladytrop/resto-payment-service:latest ./services/payment-service
docker build -t vladytrop/resto-api-gateway:latest ./services/api-gateway

# 3. Load into Kind
kind load docker-image vladytrop/resto-order-service:latest --name resto-saas
kind load docker-image vladytrop/resto-loyalty-service:latest --name resto-saas
kind load docker-image vladytrop/resto-payment-service:latest --name resto-saas
kind load docker-image vladytrop/resto-api-gateway:latest --name resto-saas

# 4. Apply manifests
kubectl apply -f infra/k8s/hpa.yml
kubectl apply -f infra/k8s/  # all other manifests

# 5. Port forward for testing
kubectl port-forward -n resto-saas svc/api-gateway 8080:8080 &
🔥 Load Testing (3000 RPS)
Install k6: https://k6.io/docs/get-started/installation/
# Run test
k6 run infra/k6/load-test-3000rps.js

# Expected output:
# ✓ GET status is 200
# ✓ GET response time < 200ms
# ✓ POST status is 200
# ✓ POST response time < 500ms
# http_reqs: 180000+ (3000 RPS sustained)
🧪 Redis vs PostgreSQL Cache Test
chmod +x scripts/cache-test.sh
./scripts/cache-test.sh

# Expected:
# Cold (Postgres): ~15-40ms/req
# Warm (Redis):    ~2-5ms/req
# Speedup: 5-10x faster with Redis
📊 Grafana Dashboard
1.	Open Grafana (port-forward if needed)
2.	Dashboard auto-imported via provisioning
3.	Panels: RPS by Endpoint, P95 Latency, JVM Heap, Kafka Events
      🔐 CI/CD Secrets (GitHub)
      Go to Settings → Secrets and variables → Actions:
      Secret	Value
      DOCKER_USERNAME	your Docker Hub username
      DOCKER_HUB_TOKEN	Docker Hub access token
      KUBECONFIG	cat ~/.kube/config | base64
      ⚠️ Known Limitations (for resume honesty)
      •	Kind is dev-only. For production use managed k8s (EKS/GKE/Yandex).
      •	HPA minReplicas: 3 — on MacBook may OOM. Reduce to 1-2 for local testing.
      •	Postgres/Redis/Kafka use emptyDir. For production add PVC.
      •	No TLS — add cert-manager for production.
      •	Rate Limiting is per-tenant (by X-Tenant-ID), not global IP-based.
