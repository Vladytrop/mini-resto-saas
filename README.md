# Mini-Resto-SaaS

Облачная SaaS-платформа для автоматизации ресторанных сетей. Пет-проект.

## Стек
- Java 21
- Spring Boot 3.4
- Gradle 8.11.1 (Kotlin DSL)
- H2 (временно, до подключения PostgreSQL)

## Структура
```
mini-resto-saas/
├── settings.gradle.kts
├── build.gradle.kts
├── services/
│   ├── shared/          # TenantContext, TenantFilter, ApiResponse
│   └── order-service/     # REST API для заказов
```

## Быстрый старт

```bash
# Собрать
./gradlew clean build

# Запустить order-service
./gradlew :services:order-service:bootRun
```

## API

```bash
# Создать заказ (обязательно заголовок X-Tenant-ID)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{"customerName":"Ivan","totalAmount":100}'

# Получить все заказы
curl http://localhost:8081/api/orders -H "X-Tenant-ID: tenant-1"

# Обновить статус
curl -X PATCH "http://localhost:8081/api/orders/{id}/status?status=CONFIRMED" \
  -H "X-Tenant-ID: tenant-1"
```
