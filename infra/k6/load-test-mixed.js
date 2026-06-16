import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8081';
const TENANT_ID = 'tenant-1';

export default function () {
    const headers = { 'X-Tenant-ID': TENANT_ID };

    if (Math.random() < 0.8) {
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

    sleep(0.1);  // 100ms pause
}