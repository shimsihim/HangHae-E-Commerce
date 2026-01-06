// ==========================================
// K6 Load Testing Script
// ==========================================
// E-Commerce API 부하 테스트
//
// 실행 방법:
// docker exec ecommerce-api-k6 k6 run /scripts/load-test.js
//
// 상세 로그와 함께 실행:
// docker exec ecommerce-api-k6 k6 run --log-output=stdout /scripts/load-test.js
//
// Grafana에서 결과 확인:
// http://localhost:3000

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

// ==========================================
// 커스텀 메트릭
// ==========================================
const errorRate = new Rate('errors');
const scenarioCounter = new Counter('scenario_executions');

// 로깅 옵션
const ENABLE_LOGGING = __ENV.ENABLE_LOGGING !== 'false'; // 기본값: true
const LOG_ERRORS_ONLY = __ENV.LOG_ERRORS_ONLY === 'true'; // 기본값: false

// 로그 헬퍼 함수
function logInfo(scenario, message, data = {}) {
  if (ENABLE_LOGGING && !LOG_ERRORS_ONLY) {
    console.log(`[INFO] [${scenario}] ${message}`, data.status ? `- Status: ${data.status}` : '', data.duration ? `- Duration: ${data.duration.toFixed(2)}ms` : '');
  }
}

function logError(scenario, message, data = {}) {
  if (ENABLE_LOGGING) {
    console.error(`[ERROR] [${scenario}] ${message}`, JSON.stringify(data, null, 2));
  }
}

function logSuccess(scenario, message, data = {}) {
  if (ENABLE_LOGGING && !LOG_ERRORS_ONLY) {
    console.log(`[SUCCESS] [${scenario}] ${message}`, `Status: ${data.status}, Duration: ${data.duration.toFixed(2)}ms`);
  }
}

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

// API 응답 검증 헬퍼
function isApiSuccess(response) {
  try {
    const body = JSON.parse(response.body);
    return body.isSuccess === true;
  } catch (e) {
    return false;
  }
}

function getErrorInfo(response) {
  try {
    const body = JSON.parse(response.body);
    return {
      errorCode: body.errorCode || 'UNKNOWN',
      message: body.message || 'No message',
    };
  } catch (e) {
    return {
      errorCode: 'PARSE_ERROR',
      message: 'Failed to parse response body',
    };
  }
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
// 테스트 시작 시 실행
// ==========================================
export function setup() {
  console.log('\n==============================================');
  console.log('🚀 K6 부하 테스트 시작');
  console.log('==============================================');
  console.log(`Target URL: ${BASE_URL}`);
  console.log(`Logging Enabled: ${ENABLE_LOGGING}`);
  console.log(`Log Errors Only: ${LOG_ERRORS_ONLY}`);
  console.log('==============================================\n');

  // 애플리케이션 상태 확인
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  if (healthCheck.status === 200) {
    console.log('✅ 애플리케이션 상태: 정상');
  } else {
    console.warn('⚠️ 애플리케이션 상태 확인 실패:', healthCheck.status);
  }
  console.log('');
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
    const scenario = '상품목록조회';

    logInfo(scenario, `요청 시작 - page=${page}, size=${size}`);
    const res = http.get(`${BASE_URL}/api/product?page=${page}&size=${size}`);

    const success = check(res, {
      'HTTP 응답 성공': (r) => r.status === 200,
      '상품 목록 조회 성공': (r) => r.status === 200 && isApiSuccess(r),
      '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    });

    if (success && isApiSuccess(res)) {
      logSuccess(scenario, '요청 성공', { status: res.status, duration: res.timings.duration });
    } else {
      const errorInfo = getErrorInfo(res);
      logError(scenario, '요청 실패', {
        status: res.status,
        duration: res.timings.duration,
        errorCode: errorInfo.errorCode,
        message: errorInfo.message
      });
    }

    scenarioCounter.add(1, { scenario });
    errorRate.add(!isApiSuccess(res));
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 2: 상품 상세 조회 (25% 비율)
  // ==========================================
  if (Math.random() < 0.25) {
    const productId = randomProductId();
    const res = http.get(`${BASE_URL}/api/product/${productId}`);

    const success = check(res, {
      'HTTP 응답 성공': (r) => r.status === 200,
      '상품 상세 조회 성공': (r) => r.status === 200 && isApiSuccess(r),
      '응답 시간 < 150ms': (r) => r.timings.duration < 150,
    });

    if (!isApiSuccess(res)) {
      const errorInfo = getErrorInfo(res);
      logError('상품상세조회', '요청 실패', {
        status: res.status,
        errorCode: errorInfo.errorCode,
        message: errorInfo.message
      });
    }

    errorRate.add(!isApiSuccess(res));
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 3: 인기 상품 조회 (15% 비율)
  // ==========================================
  if (Math.random() < 0.15) {
    const res = http.get(`${BASE_URL}/api/product/popular`);

    const success = check(res, {
      'HTTP 응답 성공': (r) => r.status === 200,
      '인기 상품 조회 성공': (r) => r.status === 200 && isApiSuccess(r),
      '응답 시간 < 300ms': (r) => r.timings.duration < 300,
    });

    if (!isApiSuccess(res)) {
      const errorInfo = getErrorInfo(res);
      logError('인기상품조회', '요청 실패', {
        status: res.status,
        errorCode: errorInfo.errorCode,
        message: errorInfo.message
      });
    }

    errorRate.add(!isApiSuccess(res));
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 4: 쿠폰 목록 조회 (10% 비율)
  // ==========================================
  if (Math.random() < 0.1) {
    const res = http.get(`${BASE_URL}/api/coupons`);

    const success = check(res, {
      'HTTP 응답 성공': (r) => r.status === 200,
      '쿠폰 목록 조회 성공': (r) => r.status === 200 && isApiSuccess(r),
      '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    });

    if (!isApiSuccess(res)) {
      const errorInfo = getErrorInfo(res);
      logError('쿠폰목록조회', '요청 실패', {
        status: res.status,
        errorCode: errorInfo.errorCode,
        message: errorInfo.message
      });
    }

    errorRate.add(!isApiSuccess(res));
    sleep(0.5);
    return;
  }

  // ==========================================
  // 시나리오 5: 쿠폰 발급 (10% 비율)
  // ==========================================
  if (Math.random() < 0.1) {
    const couponId = randomCouponId();
    const scenario = '쿠폰발급';

    const payload = JSON.stringify({
      userId: userId,
      couponId: couponId,
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    logInfo(scenario, `쿠폰 발급 요청 - userId=${userId}, couponId=${couponId}`);
    const issueRes = http.post(`${BASE_URL}/api/coupon/issue`, payload, params);

    const issueSuccess = check(issueRes, {
      '쿠폰 발급 요청 성공': (r) => r.status === 200,
      '응답 시간 < 500ms': (r) => r.timings.duration < 500,
    });

    if (issueRes.status === 200) {
      // 비동기 처리를 위해 잠시 대기 (Consumer가 처리할 시간)
      sleep(0.5);

      // 사용자 쿠폰 목록 조회로 실제 발급 여부 확인
      const checkRes = http.get(`${BASE_URL}/api/coupon/${userId}`);

      if (checkRes.status === 200 && isApiSuccess(checkRes)) {
        try {
          const body = JSON.parse(checkRes.body);
          const userCoupons = body.data || [];
          const hasCoupon = userCoupons.some(c => c.couponId === couponId);

          if (hasCoupon) {
            logSuccess(scenario, `쿠폰 발급 확인 성공 - couponId=${couponId}`, {
              status: 200,
              duration: issueRes.timings.duration
            });
          } else {
            logInfo(scenario, `쿠폰 미발급 (재고 소진 또는 이미 발급됨) - couponId=${couponId}`, {
              userCouponsCount: userCoupons.length
            });
          }
        } catch (e) {
          logError(scenario, '쿠폰 목록 파싱 실패', { error: e.message });
        }
      }
    } else {
      logError(scenario, '쿠폰 발급 요청 실패', {
        status: issueRes.status,
        body: issueRes.body
      });
    }

    errorRate.add(issueRes.status !== 200); // HTTP 오류만 에러로 카운트
    sleep(0.5);
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
      'HTTP 응답 성공': (r) => r.status === 200,
      '포인트 충전 성공': (r) => r.status === 200 && isApiSuccess(r),
      '응답 시간 < 300ms': (r) => r.timings.duration < 300,
    });

    if (!isApiSuccess(res)) {
      const errorInfo = getErrorInfo(res);
      logError('포인트충전', '요청 실패', {
        status: res.status,
        errorCode: errorInfo.errorCode,
        message: errorInfo.message
      });
    }

    errorRate.add(!isApiSuccess(res));
    sleep(1);
    return;
  }

  // ==========================================
  // 시나리오 7: 주문 생성 (5% 비율 - 가장 무거운 트랜잭션)
  // ==========================================
  const scenario = '주문생성';
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

  logInfo(scenario, `요청 시작 - userId=${userId}, productOptionId=${productOptionId}, quantity=${quantity}, usePoint=${usePoint}`);
  const res = http.post(`${BASE_URL}/api/order`, payload, params);

  const success = check(res, {
    'HTTP 응답 성공': (r) => r.status === 200,
    '응답 시간 < 1000ms': (r) => r.timings.duration < 1000,
  });

  if (res.status === 200 && isApiSuccess(res)) {
    logSuccess(scenario, '주문 생성 성공', { status: res.status, duration: res.timings.duration });
  } else if (res.status === 200 && !isApiSuccess(res)) {
    const errorInfo = getErrorInfo(res);
    logInfo(scenario, '주문 생성 실패 (예상된 비즈니스 오류)', {
      status: res.status,
      duration: res.timings.duration,
      errorCode: errorInfo.errorCode,
      message: errorInfo.message
    });
  } else {
    const errorInfo = getErrorInfo(res);
    logError(scenario, '주문 생성 실패 (예상치 못한 HTTP 오류)', {
      status: res.status,
      duration: res.timings.duration,
      errorCode: errorInfo.errorCode,
      message: errorInfo.message
    });
  }

  scenarioCounter.add(1, { scenario });
  errorRate.add(res.status !== 200); // HTTP 오류만 에러로 카운트
  sleep(2);
}

// ==========================================
// 테스트 종료 시 실행
// ==========================================
export function teardown(data) {
  console.log('\n==============================================');
  console.log('🏁 K6 부하 테스트 종료');
  console.log('==============================================\n');
}

// ==========================================
// 테스트 종료 후 요약
// ==========================================
export function handleSummary(data) {
  console.log('\n📊 테스트 결과 요약:');
  console.log(`   총 요청 수: ${data.metrics.http_reqs.values.count}`);
  console.log(`   실패율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`);
  console.log(`   평균 응답시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
  console.log(`   P95 응답시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`);

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

  // 안전하게 메트릭 접근
  const safeValue = (value) => (value !== undefined && value !== null) ? value.toFixed(2) : 'N/A';

  summary += indent + `Total Requests: ${data.metrics.http_reqs?.values?.count || 0}\n`;
  summary += indent + `Request Rate: ${safeValue(data.metrics.http_reqs?.values?.rate)} req/s\n`;
  summary += indent + `Failed Requests: ${safeValue(data.metrics.http_req_failed?.values?.rate)}%\n\n`;

  summary += indent + 'Response Times:\n';
  summary += indent + `  - Min: ${safeValue(data.metrics.http_req_duration?.values?.min)}ms\n`;
  summary += indent + `  - Avg: ${safeValue(data.metrics.http_req_duration?.values?.avg)}ms\n`;
  summary += indent + `  - P95: ${safeValue(data.metrics.http_req_duration?.values?.['p(95)'])}ms\n`;
  summary += indent + `  - P99: ${safeValue(data.metrics.http_req_duration?.values?.['p(99)'])}ms\n`;
  summary += indent + `  - Max: ${safeValue(data.metrics.http_req_duration?.values?.max)}ms\n\n`;

  summary += indent + '==========================================\n';
  summary += indent + 'View detailed results in Grafana:\n';
  summary += indent + 'http://localhost:3000\n';
  summary += indent + '==========================================\n';

  return summary;
}
