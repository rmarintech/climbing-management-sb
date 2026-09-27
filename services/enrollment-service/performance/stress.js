import http from 'k6/http';
import { check } from 'k6';

//"Let 5-10-20-30-40-50 users loop as fast as they can."
export const options = {
    stages: [
        { duration: '20s', target: 5 },
        { duration: '20s', target: 10 },
        { duration: '20s', target: 20 },
        { duration: '20s', target: 30 },
        { duration: '20s', target: 40 },
        { duration: '20s', target: 50 },
        { duration: '10s', target: 0 },
    ],

    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'p(90)',
        'p(95)',
        'p(99)',
        'max',
    ],

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {

    const response = http.get(
        'http://localhost:8081/api/v2/enrollments',
        {
            headers: {
                Authorization: `Bearer ${__ENV.TOKEN}`,
                Accept: 'application/json',
            },
        }
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}