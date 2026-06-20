#!/bin/bash
# ============================================================
# Cache Test: Redis vs PostgreSQL latency comparison
# Run this while services are up in k8s/Kind
# ============================================================

set -e

GATEWAY="http://localhost:8080"
TENANT="tenant-1"
CUSTOMER="cache-test-customer"
LOOPS=100

echo "=== Cache Test: Redis vs PostgreSQL ==="
echo "Gateway: $GATEWAY"
echo "Loops: $LOOPS"
echo ""

# 1. Warm-up: create a customer (POST)
echo "[1/4] Warm-up: creating test customer..."
curl -s -X POST "$GATEWAY/api/orders" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: $TENANT" \
    -d "{"customerName":"$CUSTOMER","totalAmount":100}" > /dev/null
echo "Done."

# 2. Cold read (should hit Postgres)
echo ""
echo "[2/4] Cold read (Postgres, no cache)..."
START=$(date +%s%N)
for i in $(seq 1 $LOOPS); do
    curl -s -o /dev/null -w "%{http_code}" \
        "$GATEWAY/api/orders" \
        -H "X-Tenant-ID: $TENANT" > /dev/null
done
END=$(date +%s%N)
COLD_MS=$(( (END - START) / 1000000 ))
COLD_AVG=$(( COLD_MS / LOOPS ))
echo "Total: ${COLD_MS}ms | Avg per request: ${COLD_AVG}ms"

# 3. Warm read (should hit Redis cache)
echo ""
echo "[3/4] Warm read (Redis cache)..."
START=$(date +%s%N)
for i in $(seq 1 $LOOPS); do
    curl -s -o /dev/null -w "%{http_code}" \
        "$GATEWAY/api/orders" \
        -H "X-Tenant-ID: $TENANT" > /dev/null
done
END=$(date +%s%N)
WARM_MS=$(( (END - START) / 1000000 ))
WARM_AVG=$(( WARM_MS / LOOPS ))
echo "Total: ${WARM_MS}ms | Avg per request: ${WARM_AVG}ms"

# 4. Results
echo ""
echo "=== Results ==="
echo "Cold (Postgres): ${COLD_AVG}ms/req"
echo "Warm (Redis):    ${WARM_AVG}ms/req"
if [ "$COLD_AVG" -gt 0 ]; then
    SPEEDUP=$(( (COLD_AVG * 100) / WARM_AVG ))
    echo "Speedup: ${SPEEDUP}x faster with Redis"
else
    echo "Speedup: N/A (cold was too fast)"
fi

echo ""
echo "=== Prometheus metrics check ==="
echo "Redis hits:   curl -s localhost:8080/actuator/prometheus | grep redis"
echo "DB queries:   curl -s localhost:8080/actuator/prometheus | grep hikaricp"
echo ""
echo "Tip: Open Grafana dashboard to see RPS drop on Postgres and spike on Redis."