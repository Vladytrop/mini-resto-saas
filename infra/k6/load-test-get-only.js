import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8081';
const TENANT_ID = 'tenant-1';

export default function () {
    const res = http.get(`${BASE_URL}/api/orders`, {
        headers: { 'X-Tenant-ID': TENANT_ID },
    });

    check(res, { 'GET 200': (r) => r.status === 200 });

    sleep(0.1);  // ← 100ms между запросами, 10 RPS на VU
}