import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 5 },     // Разогрев
        { duration: '30s', target: 10 },    // Наращивание
        { duration: '1m',  target: 20 },    // Основная нагрузка
        { duration: '30s', target: 0 },     // Спад
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],  // P95 < 1 сек
        http_req_failed: ['rate<0.05'],       // Ошибки < 5%
    },
};

const BASE_URL = 'http://api-gateway:8080'; // Внутри кластера
const TENANT_ID = 'tenant-1';

function randomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

export default function () {
    const headers = {
        'Content-Type': 'application/json',
        'X-Tenant-Id': TENANT_ID, // Важно: именно так пишется в API
    };

    const rand = Math.random();

    if (rand < 0.8) {
        // 80% GET — чтение (Redis + PostgreSQL RLS)
        const res = http.get(`${BASE_URL}/api/orders`, { headers });
        check(res, {
            'GET status is 200': (r) => r.status === 200,
            'GET response time < 500ms': (r) => r.timings.duration < 500,
        });
    } else {
        // 20% POST — запись (Kafka + PostgreSQL)
        const payload = JSON.stringify({
            customerName: `Load-${randomString(8)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });
        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
        check(res, {
            'POST status is 201': (r) => r.status === 201 || r.status === 200,
            'POST response time < 1000ms': (r) => r.timings.duration < 1000,
        });
    }

    sleep(0.1); // 100 мс пауза между итерациями
}