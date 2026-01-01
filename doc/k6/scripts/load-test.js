// ==========================================
// K6 Load Testing Script
// ==========================================
// E-Commerce API 부하 테스트
//
// 실행 방법:
// docker exec -it ecommerce-api-k6 k6 run /scripts/load-test.js
//
// Grafana에서 결과 확인:
// http://localhost:3000

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// ==========================================
// 커스텀 메트릭
// ==========================================
const errorRate = new Rate('errors');

// ==========================================
// 테스트 시나리오 옵션
// ==========================================
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp-up: 30초 동안 10명까지 증가
    { duration: '1m', target: 50 },    // Load: 1분 동안 50명 유지
    { duration: '30s', target: 100 },  // Spike: 30초 동안 100명까지 증가
    { duration: '1m', target: 50 },    // Recovery: 1분 동안 50명으로 감소
    { duration: '30s', target: 0 },    // Ramp-down: 30초 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95%의 요청이 500ms 이내
    http_req_failed: ['rate<0.1'],     // 에러율 10% 미만
    errors: ['rate<0.1'],              // 커스텀 에러율 10% 미만
  },
};

// ==========================================
// 환경 설정
// ==========================================
// Docker 네트워크 내부: http://host.docker.internal:8080
// 로컬 테스트: http://localhost:8080
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// ==========================================
// 헬퍼 함수
// ==========================================
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomUserId() {
  return randomInt(1, 10);  // 사용자 1~10
}

function randomProductId() {
  return randomInt(1, 10);  // 상품 1~10
}

function randomCouponId() {
  return randomInt(1, 5);   // 쿠폰 1~5
}

// ==========================================
// 메인 테스트 시나리오
// ==========================================
export default function () {
  const userId = randomUserId();

  // ==========================================
  // 시나리오 1: 상품 목록 조회 (30% 비율)
  // ==========================================
  if (Math.random() < 0.3) {
    const page = randomInt(0, 3);
    const size = 10;
    const res = http.get(`${BASE_URL}/api/products?page=${page}&size=${size}`);

    const success = check(res, {
      '상품 목록 조회 성공': (r) => r.status === 200,
      '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    });

    errorRate.add(!success);
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 2: 상품 상세 조회 (25% 비율)
  // ==========================================
  if (Math.random() < 0.25) {
    const productId = randomProductId();
    const res = http.get(`${BASE_URL}/api/products/${productId}`);

    const success = check(res, {
      '상품 상세 조회 성공': (r) => r.status === 200,
      '응답 시간 < 150ms': (r) => r.timings.duration < 150,
    });

    errorRate.add(!success);
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 3: 인기 상품 조회 (15% 비율)
  // ==========================================
  if (Math.random() < 0.15) {
    const res = http.get(`${BASE_URL}/api/products/popular`);

    const success = check(res, {
      '인기 상품 조회 성공': (r) => r.status === 200,
      '응답 시간 < 300ms': (r) => r.timings.duration < 300,
    });

    errorRate.add(!success);
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 4: 쿠폰 목록 조회 (10% 비율)
  // ==========================================
  if (Math.random() < 0.1) {
    const res = http.get(`${BASE_URL}/api/coupons`);

    const success = check(res, {
      '쿠폰 목록 조회 성공': (r) => r.status === 200,
      '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    });

    errorRate.add(!success);
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 5: 쿠폰 발급 (10% 비율)
  // ==========================================
  if (Math.random() < 0.1) {
    const couponId = randomCouponId();
    const payload = JSON.stringify({
      userId: userId,
      couponId: couponId,
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const res = http.post(`${BASE_URL}/api/coupons/issue`, payload, params);

    const success = check(res, {
      '쿠폰 발급 성공 또는 실패': (r) => r.status === 200 || r.status === 400,
      '응답 시간 < 500ms': (r) => r.timings.duration < 500,
    });

    errorRate.add(!success);
    sleep(1);
    return;
  }

  // ==========================================
  // 시나리오 6: 포인트 충전 (5% 비율)
  // ==========================================
  if (Math.random() < 0.05) {
    const amount = randomInt(10, 100) * 1000;  // 1만원 ~ 10만원
    const payload = JSON.stringify({
      userId: userId,
      amount: amount,
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const res = http.post(`${BASE_URL}/api/point/charge`, payload, params);

    const success = check(res, {
      '포인트 충전 성공': (r) => r.status === 200,
      '응답 시간 < 300ms': (r) => r.timings.duration < 300,
    });

    errorRate.add(!success);
    sleep(1);
    return;
  }

  // ==========================================
  // 시나리오 7: 주문 생성 (5% 비율 - 가장 무거운 트랜잭션)
  // ==========================================
  const productOptionId = randomInt(1, 23);  // 상품 옵션 1~23
  const quantity = randomInt(1, 3);
  const usePoint = randomInt(0, 5) * 1000;   // 0원 ~ 5만원

  const payload = JSON.stringify({
    userId: userId,
    orderItems: [
      {
        productOptionId: productOptionId,
        quantity: quantity,
      },
    ],
    usePoint: usePoint,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(`${BASE_URL}/api/orders`, payload, params);

  const success = check(res, {
    '주문 생성 성공 또는 실패': (r) => r.status === 200 || r.status === 400,
    '응답 시간 < 1000ms': (r) => r.timings.duration < 1000,
  });

  errorRate.add(!success);
  sleep(2);
}

// ==========================================
// 테스트 종료 후 요약
// ==========================================
export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  const colors = options.enableColors || false;

  let summary = '\n';
  summary += indent + '==========================================\n';
  summary += indent + 'K6 Load Test Summary\n';
  summary += indent + '==========================================\n\n';

  summary += indent + `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += indent + `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s\n`;
  summary += indent + `Failed Requests: ${data.metrics.http_req_failed.values.rate.toFixed(2)}%\n\n`;

  summary += indent + 'Response Times:\n';
  summary += indent + `  - Min: ${data.metrics.http_req_duration.values.min.toFixed(2)}ms\n`;
  summary += indent + `  - Avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
  summary += indent + `  - P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
  summary += indent + `  - P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
  summary += indent + `  - Max: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n\n`;

  summary += indent + '==========================================\n';
  summary += indent + 'View detailed results in Grafana:\n';
  summary += indent + 'http://localhost:3000\n';
  summary += indent + '==========================================\n';

  return summary;
}
