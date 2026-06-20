import http from 'k6/http';
import { check, sleep } from 'k6';

// Target: 3000 RPS sustained
// With 100 VU and no sleep → ~1000-3000 RPS depending on latency
// With 300 VU and sleep(0.05) → ~3000 RPS (20 req/s per VU * 300 = 6000 peak,
// but limited by system capacity, settles around 3000)
export const options = {
    stages: [
        { duration: '30s', target: 50 },     // Ramp up: warm-up
        { duration: '30s', target: 150 },    // Ramp up: 150 VU
        { duration: '1m', target: 300 },     // Steady: 300 VU → ~3000 RPS
        { duration: '2m', target: 300 },    // Sustained load
        { duration: '30s', target: 0 },      // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],   // P95 < 500ms
        http_req_failed: ['rate<0.01'],       // Errors < 1%
        http_reqs: ['count>180000'],           // 3000 RPS * 60s = 180k req in peak minute
    },
};

const BASE_URL = 'http://localhost:8080';  // Gateway
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

    // 80% GET (cache/DB read), 20% POST (write + Kafka + RLS)
    const rand = Math.random();

    if (rand < 0.8) {
        // GET /api/orders — test caching + RLS
        const res = http.get(`${BASE_URL}/api/orders`, { headers });

        check(res, {
            'GET status is 200': (r) => r.status === 200,
            'GET response time < 200ms': (r) => r.timings.duration < 200,
        });
    } else {
        // POST /api/orders — test write throughput + Kafka
        const payload = JSON.stringify({
            customerName: `Load-${randomString(8)}`,
            totalAmount: Math.floor(Math.random() * 500) + 50,
        });

        const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });

        check(res, {
            'POST status is 200': (r) => r.status === 200,
            'POST response time < 500ms': (r) => r.timings.duration < 500,
        });
    }

    // 300 VU * (1/0.05)s = ~6000 req/s theoretical max
    // Actual will be limited by system latency, settling around 3000 RPS
    sleep(0.05);  // 50ms pause = ~20 req/s per VU
}