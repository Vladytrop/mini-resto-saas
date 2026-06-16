import http from 'k6/http';
import { check } from 'k6';

// k6: 100 VU, 2 минуты
export const options = {
    stages: [
        { duration: '30s', target: 200 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.05'],
    },
};

const BASE_URL = 'http://localhost:8081';
const TENANT_ID = 'tenant-1';

export default function () {
    const headers = {
        'Content-Type': 'application/json',
        'X-Tenant-ID': TENANT_ID,
    };

    // 80% GET (кэш), 20% POST
    const rand = Math.random();

    if (rand < 0.8) {
        const res = http.get(`${BASE_URL}/api/orders`, { headers });
        check(res, { 'GET 200': (r) => r.status === 200 });
    } else {
        const payload = JSON.stringify({
            customerName: `Load-${Math.random().toString(36).substring(7)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });
        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
        check(res, { 'POST 200': (r) => r.status === 200 });
    }

    // sleep(1); УБРАТЬ! VU спамит запросы без паузы
    const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
}