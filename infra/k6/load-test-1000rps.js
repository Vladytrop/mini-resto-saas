import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';
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
            'GET response time < 200ms': (r) => r.timings.duration < 200,
        });
    } else {
        const payload = JSON.stringify({
            customerName: `Load-${randomString(8)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });
        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
        check(res, {
            'POST status is 201': (r) => r.status === 201 || r.status === 200,
            'POST response time < 500ms': (r) => r.timings.duration < 500,
        });
    }
    sleep(0.05);
}
