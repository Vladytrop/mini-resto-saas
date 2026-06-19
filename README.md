# Mini-Resto-SaaS

Облачная SaaS-платформа для автоматизации ресторанных сетей. Пет-проект, демонстрирующий production-паттерны в мини-версии.

# Architecture
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│   API Gateway   │────▶│ Rate Limiter │────▶│  Order Service  │
│   (port 8080)   │     │  (Redis)     │     │   (port 8081)   │
└─────────────────┘     └──────────────┘     └────────┬────────┘
│
┌───────────────────────┼──────────┐
│                       │          │
▼                       ▼          ▼
┌──────────┐          ┌──────────┐  ┌──────────┐
│  Kafka   │          │ Postgres │  │  Redis   │
│(9 parts) │          │   + RLS  │  │  Cache   │
│  RF=3    │          │  + Flyway│  │          │
└────┬─────┘          └──────────┘  └──────────┘
│
▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│Loyalty Svc   │      │Payment Svc   │      │Analytics Svc │      │ Prometheus   │
│(port 8082)   │      │(port 8083)   │      │(port 8084)   │      │  + Grafana   │
│ 5 000 RPS    │      │ 8 providers  │      │  Reports     │      │  Dashboards  │
└──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘
plain

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4, Spring Cloud, Spring MVC |
| Database | PostgreSQL 16 (RLS, partitioning) |
| Cache | Redis (TTL, pub/sub invalidation) |
| Messaging | Apache Kafka (12 partitions, RF=3) |
| Migrations | Flyway |
| Monitoring | Prometheus + Grafana |
| Resilience | Resilience4j (Circuit Breaker) |
| CI/CD | GitHub Actions + Docker |
| Orchestration | Kubernetes + HPA |
| Testing | JUnit, Mockito |

## Quick Start

```bash
# 1. Clone
git clone https://github.com/vladytrop/mini-resto-saas.git
cd mini-resto-saas

# 2. Start infrastructure
docker-compose up -d postgres redis kafka

# 3. Start services
./gradlew :services:order-service:bootRun
./gradlew :services:loyalty-service:bootRun
./gradlew :services:payment-service:bootRun
./gradlew :services:analytics-service:bootRun

# 4. API Gateway
./gradlew :services:api-gateway:bootRun
API Examples
bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"customerName":"John Doe","totalAmount":100.00}'

# Get loyalty points
curl http://localhost:8080/api/loyalty/balance/John%20Doe \
  -H "X-Tenant-ID: tenant-1"

# Analytics — top customers
curl http://localhost:8084/api/analytics/top-customers

# Analytics — revenue by tenant
curl http://localhost:8084/api/analytics/revenue-by-tenant
Kubernetes Deployment
bash
# Apply all manifests
kubectl apply -f infra/k8s/

# Check HPA
kubectl get hpa -n resto-saas

# Port forward for local access
kubectl port-forward svc/api-gateway 8080:8080 -n resto-saas
Project Structure
plain
mini-resto-saas/
├── services/
│   ├── shared/              # Common: RLS, Flyway, DTOs
│   ├── api-gateway/         # Spring Cloud Gateway
│   ├── order-service/       # Orders + Redis cache
│   ├── loyalty-service/     # Loyalty program (5K RPS)
│   ├── payment-service/     # 8 payment providers + CB
│   └── analytics-service/   # Prometheus metrics + reports
├── infra/
│   ├── docker-compose.yml   # Local dev
│   ├── k8s/                 # Kubernetes manifests
│   └── grafana/             # Dashboard JSON
└── .github/workflows/
    └── build.yml            # CI/CD: build, test, Docker push
CI/CD
https://github.com/vladytrop/mini-resto-saas/actions/workflows/build.yml/badge.svg
Automated build & test on push
Docker images pushed to Docker Hub
Ready for Kubernetes deployment
Key Features
Multi-tenancy: Row-Level Security in PostgreSQL
Event-driven: Kafka for order/payment events
Caching: Redis with TTL and pub/sub invalidation
Resilience: Circuit Breaker for 8 payment providers
Auto-scaling: HPA in Kubernetes (2-10 replicas)
Observability: Prometheus metrics + Grafana dashboards
License
MIT