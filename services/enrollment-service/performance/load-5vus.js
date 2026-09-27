import http from 'k6/http';
import { check } from 'k6';

//"Let 5 users loop as fast as they can."
export const options = {
    vus: 5,
    duration: '30s',

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