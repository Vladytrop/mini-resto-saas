// 5000 rps
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },   // Разогрев
        { duration: '30s', target: 300 },   // Наращивание
        { duration: '2m',  target: 500 },   // Пик: ~5000 RPS
        { duration: '30s', target: 0 },     // Спад
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],   // P95 < 500 мс
        http_req_failed: ['rate<0.01'],      // Ошибки < 1%
    },
};

const BASE_URL = 'http://api-gateway:8080';
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
        'X-Tenant-Id': TENANT_ID,
    };

    const rand = Math.random();

    if (rand < 0.8) {
        const res = http.get(`${BASE_URL}/api/orders`, { headers });
        check(res, {
            'GET status is 200': (r) => r.status === 200,
            'GET p95 < 300ms': (r) => r.timings.duration < 300,
        });
    } else {
        const payload = JSON.stringify({
            customerName: `Load-${randomString(8)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });
        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
        check(res, {
            'POST status is 201': (r) => r.status === 201 || r.status === 200,
            'POST p95 < 800ms': (r) => r.timings.duration < 800,
        });
    }
    // sleep убран — максимальный throughput
}