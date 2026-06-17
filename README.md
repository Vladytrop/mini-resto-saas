# Mini-Resto-SaaS

Облачная SaaS-платформа для автоматизации ресторанных сетей. Пет-проект, демонстрирующий production-паттерны в мини-версии.

## Архитектура
┌─────────────────────────────────────────┐
│  Client (HTTP + X-Tenant-ID header)    │
└──────────────────┬──────────────────────┘
│
▼
┌─────────────────────────────────────────┐
│  API Gateway (Spring Cloud Gateway)      │
│  Port: 8080                            │
└──────────────────┬──────────────────────┘
│
┌───────────┴───────────┐
▼                       ▼
┌─────────────┐         ┌─────────────┐
│ Order Service│         │Loyalty Service│
│ Port: 8081   │         │ Port: 8082   │
│              │         │              │
│ • REST API   │         │ • Kafka Consumer│
│ • JPA + RLS  │◄────────┤ • Loyalty points│
│ • Redis Cache│  Kafka  │               │
│ • Kafka Producer      │               │
└─────────────┘         └─────────────┘
│                       │
└───────────┬───────────┘
▼
┌─────────────────┐
│  PostgreSQL 16   │
│  RLS enabled     │
│  resto_app user  │
└─────────────────┘
│
┌─────────────────┐
│  Redis           │
│  Master/Replica  │
│  Cache + Pub/Sub │
└─────────────────┘
plain

## Стек

- Java 21, Spring Boot 3.4, Gradle 8.11.1 (Kotlin DSL)
- PostgreSQL 16 + Row-Level Security
- Redis 7 (Master/Replica, Spring Cache, Pub/Sub)
- Kafka 3.7 (KRaft, 3 брокера, 9 партиций, RF=3)
- Prometheus + Grafana
- Kubernetes манифесты (HPA, ConfigMap, Secret)

## Multi-tenancy через RLS

Каждый tenant видит только свои данные на уровне PostgreSQL. Невозможно обойти из приложения.

```sql
CREATE POLICY orders_policy ON orders
  USING (tenant_id = current_setting('app.current_tenant', true));
Event-driven (Kafka)
Order Service публикует OrderCreatedEvent. Loyalty Service начисляет баллы.
Кэширование (Redis)
@Cacheable с Jackson JSON сериализацией
Tenant-префикс в ключах (tenant-1:orders:list)
Pub/Sub инвалидация между инстансами
Запуск локально
bash

# Инфраструктура
cd infra/docker-compose
docker compose up -d

# Order Service
./gradlew :services:order-service:bootRun

# Loyalty Service
./gradlew :services:loyalty-service:bootRun

# API Gateway
./gradlew :services:api-gateway:bootRun
Kubernetes (minikube/kind)
bash
kubectl apply -f infra/k8s/
Swagger UI
Order Service: http://localhost:8081/swagger-ui.html
Loyalty Service: http://localhost:8082/swagger-ui.html
API Gateway: http://localhost:8080/swagger-ui.html (routing)
Grafana
http://localhost:3000 (admin/admin)
Dashboard: "Resto SaaS Overview" — RPS, P95 latency, JVM memory, Kafka events.