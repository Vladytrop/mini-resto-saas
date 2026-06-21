# Mini-Resto-SaaS

Облачная SaaS-платформа для автоматизации ресторанных сетей. Пет-проект, демонстрирующий production-паттерны: мультитенантность, event-driven архитектуру, кэширование, circuit breaker и Kubernetes-деплой.

---

## Архитектура

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────┐
│   API Gateway   │────▶│ Rate Limiter │────▶│  Order Service  │
│   (port 8080)   │     │  (Redis)     │     │   (port 8081)   │
└─────────────────┘     └──────────────┘     └────────┬────────┘
                                                       │
┌───────────────────────┼──────────┐                  │
│                       │          │                  │
▼                       ▼          ▼                  ▼
┌──────────┐          ┌──────────┐  ┌──────────┐  ┌──────────┐
│  Kafka   │          │ Postgres │  │  Redis   │  │ Flyway   │
│(events)  │          │   + RLS  │  │  Cache   │  │ Migrations│
└────┬─────┘          └──────────┘  └──────────┘  └──────────┘
     │
     ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│Loyalty Svc   │      │Payment Svc   │      │Analytics Svc │      │ Prometheus   │
│(port 8082)   │      │(port 8083)   │      │(port 8084)   │      │  + Grafana   │
│ Redis Cache  │      │ 8 providers  │      │  Reports     │      │  Dashboards  │
└──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4, Spring Cloud Gateway |
| Database | PostgreSQL 16 (RLS, partitioning) + Flyway |
| Cache | Redis (TTL, pub/sub invalidation) |
| Messaging | Apache Kafka |
| Monitoring | Prometheus + Grafana |
| Resilience | Resilience4j (Circuit Breaker) |
| CI/CD | GitHub Actions + Docker |
| Orchestration | Kubernetes (Kind) + HPA |
| Testing | JUnit, Mockito, k6 |

---

## Быстрый старт (Kind + Docker Desktop)

### 1. Инфраструктура (Docker Compose)

```bash
cd infra/docker-compose
docker compose up -d
```

> Если ошибка `Pool overlaps with other one` — подсеть уже изменена на `172.30.0.0/16` в `docker-compose.yml`.

### 2. Сборка и деплой в Kind

```bash
# Собрать все сервисы
./gradlew clean bootJar

# Собрать Docker-образы
docker build -t vladytrop/resto-order-service:flyway-v1 ./services/order-service
docker build -t vladytrop/resto-loyalty-service:cache-v2 ./services/loyalty-service
docker build -t vladytrop/resto-payment-service:latest ./services/payment-service
docker build -t vladytrop/resto-analytics-service:fixed-v1 ./services/analytics-service
docker build -t vladytrop/resto-api-gateway:k8s-fix9 ./services/api-gateway

# Загрузить в Kind
kind load docker-image vladytrop/resto-order-service:flyway-v1 --name resto-cluster
kind load docker-image vladytrop/resto-loyalty-service:cache-v2 --name resto-cluster
kind load docker-image vladytrop/resto-payment-service:latest --name resto-cluster
kind load docker-image vladytrop/resto-analytics-service:fixed-v1 --name resto-cluster
kind load docker-image vladytrop/resto-api-gateway:k8s-fix9 --name resto-cluster

# Применить манифесты
kubectl apply -f infra/k8s/namespaces.yml
kubectl apply -f infra/k8s/configmap.yml
kubectl apply -f infra/k8s/secret.yml
kubectl apply -f infra/k8s/postgres.yml
kubectl apply -f infra/k8s/redis.yml
kubectl apply -f infra/k8s/kafka.yml
kubectl apply -f infra/k8s/order-service.yml
kubectl apply -f infra/k8s/loyalty-service.yml
kubectl apply -f infra/k8s/payment-service.yml
kubectl apply -f infra/k8s/analytics-service.yml
kubectl apply -f infra/k8s/api-gateway.yml
kubectl apply -f infra/k8s/prometheus.yml
kubectl apply -f infra/k8s/grafana.yml
kubectl apply -f infra/k8s/hpa.yml
kubectl apply -f infra/k8s/ingress.yml

# Проверить статус
kubectl get pods -n resto-saas -w или kubectl get pods -n resto-saas
```

### 3. Доступ локально

| Сервис | URL |
|--------|-----|
| API Gateway | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

---

## API Примеры

```bash
# Создать заказ
curl -X POST http://localhost:8080/api/orders   -H "X-Tenant-ID: tenant-1"   -H "Content-Type: application/json"   -d '{"customerName":"John Doe","totalAmount":100.00}'

# Получить баланс лояльности
curl http://localhost:8080/api/loyalty/balance/John%20Doe   -H "X-Tenant-ID: tenant-1"

# Analytics — топ клиентов
curl http://localhost:8080/api/analytics/top-customers

# Analytics — выручка по тенантам
curl http://localhost:8080/api/analytics/revenue-by-tenant

# Health check gateway
curl http://localhost:8080/actuator/health
```

---

## Структура проекта

```
mini-resto-saas/
├── services/
│   ├── shared/              # Общие модули: RLS, DTOs
│   ├── api-gateway/         # Spring Cloud Gateway (профиль k8s)
│   ├── order-service/       # Заказы + Flyway миграции + Redis cache
│   ├── loyalty-service/     # Программа лояльности + Redis
│   ├── payment-service/     # 8 платёжных провайдеров + Circuit Breaker
│   └── analytics-service/   # Отчёты + Prometheus metrics
├── infra/
│   ├── docker-compose/      # Локальная инфраструктура (Postgres, Redis, Kafka)
│   ├── k8s/                 # Kubernetes манифесты
│   ├── grafana/             # Dashboard provisioning
│   └── k6/                  # Нагрузочные тесты
├── .github/workflows/
│   └── build.yml            # CI/CD: build, test, Docker push
└── kind-config.yaml         # Конфигурация Kind-кластера
```

---

## CI/CD

![Build](https://github.com/vladytrop/mini-resto-saas/actions/workflows/build.yml/badge.svg)

- Автоматическая сборка и тесты при push
- Docker-образы пушатся в Docker Hub
- Деплой в Kubernetes готов к подключению облачного кластера (EKS/GKE/AKS)

> **Примечание:** Job `deploy-to-k8s` в GitHub Actions требует `secrets.KUBECONFIG` от облачного кластера. Для локального тестирования используется Kind.

---

## Нагрузочное тестирование (k6)

```bash
# Smoke-тест
k6 run infra/k6/load-test-get-only.js

# 3000 RPS sustained
k6 run infra/k6/load-test-3000rps.js
```

Ожидаемые результаты:
- `GET` — < 200ms
- `POST` — < 500ms
- Redis cache — 5-10x быстрее PostgreSQL

---

## Известные ограничения

- **Kind** — dev-only. Kubernetes in Docker.
- **HPA** — `minReplicas: 3` может OOM. Для локального тестирования уменьшите до 1-2.
- **Персистентность** — Postgres/Redis/Kafka используют `emptyDir`.
- **TLS** — отсутствует.
- **Rate Limiting** — per-tenant (по `X-Tenant-ID`), а не global IP-based.

---
