import http from 'k6/http';
import { check, sleep } from 'k6';

// Конфигурация нагрузки
export const options = {
    stages: [
        { duration: '30s', target: 100 },   // Ramp up: 0 → 100 VU
        { duration: '1m', target: 100 },    // Steady: 100 VU
        { duration: '30s', target: 200 },   // Ramp up: 100 → 200 VU
        { duration: '1m', target: 200 },     // Steady: 200 VU
        { duration: '30s', target: 0 },      // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],   // P95 < 500ms
        http_req_failed: ['rate<0.01'],     // Ошибки < 1%
    },
};

const BASE_URL = 'http://localhost:8081';
const TENANT_ID = 'tenant-1';

// Генератор случайных данных
function randomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

export default function () {
    const headers = {
        'Content-Type': 'application/json',
        'X-Tenant-ID': TENANT_ID,
    };

    // 70% GET (чтение кэша/БД), 30% POST (запись + Kafka)
    const rand = Math.random();

    if (rand < 0.7) {
        // GET /api/orders — проверяем кэширование
        const res = http.get(`${BASE_URL}/api/orders`, { headers });

        check(res, {
            'GET status is 200': (r) => r.status === 200,
            'GET response time < 200ms': (r) => r.timings.duration < 200,
        });
    } else {
        // POST /api/orders — проверяем запись + Kafka + RLS
        const payload = JSON.stringify({
            customerName: `LoadTest-${randomString(8)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });

        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });

        check(res, {
            'POST status is 200': (r) => r.status === 200,
            'POST response time < 500ms': (r) => r.timings.duration < 500,
        });
    }

    sleep(1); // 1 запрос в секунду на VU
}