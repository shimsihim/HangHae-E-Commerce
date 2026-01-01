# Senior Engineer's Master Development Guide

## 1. 아키텍처 철학 (UseCase Driven)
- **Fat Service 방지**: 모든 비즈니스 로직은 단일 책임을 가진 `UseCase` 클래스로 분리한다. (예: `PlaceOrderUseCase`)
- **MSA Ready**: 모놀로식 내에서 도메인 간 참조는 인터페이스나 이벤트를 통해서만 수행하며, DB 조인은 도메인 경계를 넘지 않는다.
- **Rich Domain Model**: 엔티티는 상태와 행위를 모두 가지며, 스스로의 비즈니스 무결성을 책임진다. 외부에서 필드를 직접 변경하는 대신 의미 있는 비즈니스 메서드를 제공한다.

## 2. 데이터 정합성 및 이벤트 (Outbox Pattern)
- **Transactional Outbox**: 모든 외부 메시지(Kafka) 발행은 로컬 트랜잭션 내에서 Outbox 테이블 저장을 선행한다.
- **Reliability**: `TransactionalEventListener(phase = AFTER_COMMIT)`를 사용하여 DB 커밋 성공 시에만 이벤트를 발행한다.
- **Idempotency**: 소비자(Consumer)는 중복 메시지 처리를 방지하기 위한 멱등성 로직을 반드시 구현한다.

## 3. Gemini CLI 협업 프로토콜 (Cross-Validation)
Claude는 코딩 전/후에 반드시 `gemini` 명령어로 다음을 수행하고 그 결과를 보고하라.
- **Step 1 (Architecture)**: SRP 위반 여부 및 UseCase 설계의 적절성 검토.
- **Step 2 (Critical Logic)**: 분산 락(Redis) 범위, 트랜잭션 전파, Kafka 복구 전략(DLQ 등) 분석.
- **Step 3 (Edge Case)**: 비즈니스 흐름상 발생 가능한 특이 케이스(재고 부족, 결제 타임아웃 등) 5가지 도출 및 방어 코드 제안.

## 4. 실무 금지 패턴 (Anti-Patterns)
다음 패턴이 발견될 경우 즉시 리팩토링 대상으로 간주한다.
- **Anemic Model**: 엔티티에 `@Setter`를 사용하지 않는다. 의도가 명확한 메서드(예: `cancel()`)를 노출한다.
- **Raw Collections**: 비즈니스 레이어에서 `Map`이나 `Set`을 날것으로 반환하지 않는다. 데이터의 의도가 명확한 DTO나 전용 일급 컬렉션을 사용한다.
- **Long-lived Transactions**: `@Transactional` 내부에서 외부 API(결제사, 알림 발송 등)를 호출하지 않는다.
- **Hard Delete**: 물리 삭제 대신 `is_deleted` 필드를 통한 Soft Delete를 기본으로 한다.

## 5. 성능 및 운영 (Senior Level Detail)
- **N+1 제어**: 연관 관계는 지연 로딩(`LAZY`)을 기본으로 하며, 성능이 필요한 곳은 Querydsl 페치 조인이나 DTO 직접 조회를 수행한다.
- **Observability**: 모든 로그에는 `Trace ID`를 포함하여 이벤트 흐름을 추적 가능하게 하고, '무엇을'이 아닌 '왜(Why)'에 대한 맥락을 담는다.
- **Timezone**: 모든 시간 처리는 `UTC`를 기준으로 저장하고, 사용자 노출 시에만 클라이언트 타임존으로 변환한다.
- **Fail-Fast**: 유효성 검증은 메서드 최상단에서 수행하여 불필요한 연산과 트랜잭션 유지를 차단한다.

## 6. 테스트 전략
- 모든 UseCase는 단위 테스트를 포함한다.
- `should_Expectation_When_Condition` 형식을 사용하여 테스트 의도를 명확히 기술한다.
- Mockito를 활용하여 외부 의존성을 격리하고 오직 비즈니스 로직의 정합성만 검증한다.

## 7. Docker 기반 개발 환경

### 7.1 인프라 스택 구성
로컬 개발 환경은 Docker Compose를 통해 다음 서비스를 제공한다:
- **MySQL 8.0**: 주 데이터베이스 (초기화 스크립트 자동 실행)
- **Redis 7**: 분산 락 및 캐싱
- **Kafka + Kafka UI**: 이벤트 메시징 (Outbox 패턴)
- **InfluxDB + Grafana + K6**: 부하 테스트 및 모니터링

### 7.2 실행 방법

#### 전체 인프라 시작
```bash
cd doc
docker-compose up -d
```

#### 서비스 접속 정보
- **Kafka UI**: http://localhost:8090
- **Grafana**: http://localhost:3000 (admin/admin)
- **MySQL**: localhost:3306 (root/admin)
- **Redis**: localhost:6379
- **InfluxDB**: localhost:8086

#### Spring Boot 애플리케이션 실행
```bash
# 로컬 모드 (기본값: localhost)
./gradlew bootRun

# Docker 네트워크 모드
DB_HOST=mysql REDIS_HOST=redis KAFKA_BOOTSTRAP_SERVERS=kafka:29092 ./gradlew bootRun
```

### 7.3 데이터베이스 초기화 전략
- **DDL 모드**: `validate` (schema.sql에서 테이블 생성)
- **초기화 스크립트**:
  - `doc/database/init/01-schema.sql`: 테이블 스키마
  - `doc/database/init/02-data.sql`: 테스트 데이터 (사용자 10명, 상품 10개 등)
- **재초기화**: `docker-compose down -v && docker-compose up -d`

### 7.4 부하 테스트 실행

#### K6 테스트 스크립트 실행
```bash
# K6 컨테이너에서 부하 테스트 실행
docker exec -it ecommerce-api-k6 k6 run /scripts/load-test.js

# 커스텀 BASE_URL 지정
docker exec -e BASE_URL=http://host.docker.internal:8080 -it ecommerce-api-k6 k6 run /scripts/load-test.js
```

#### Grafana에서 실시간 모니터링
1. http://localhost:3000 접속
2. 좌측 메뉴 > Dashboards > Import
3. K6 Dashboard ID: **2587** 입력
4. InfluxDB 데이터소스 선택 후 Import
5. 실시간 부하 테스트 메트릭 확인 (RPS, P95 응답시간 등)

### 7.5 환경별 설정 전략
**로컬 개발** (기본값):
- 환경변수 없이 실행 시 `localhost` 사용
- JPA DDL: `validate` (schema.sql 사용)

**Docker Compose 환경**:
- `DB_HOST=mysql`, `REDIS_HOST=redis`
- `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`

**개발/운영 환경**:
- 시스템 환경변수 또는 JAR 실행 시 파라미터로 설정
- 예: `java -jar app.jar --DB_HOST=prod-db --JPA_DDL_AUTO=none`

### 7.6 인프라 관리 명령어

#### 컨테이너 상태 확인
```bash
docker-compose ps
```

#### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f mysql
docker-compose logs -f kafka
```

#### 볼륨 초기화 (데이터 완전 삭제)
```bash
docker-compose down -v
```

#### 서비스 재시작
```bash
# 특정 서비스만 재시작
docker-compose restart mysql

# 전체 재시작
docker-compose restart
```

### 7.7 트러블슈팅

#### MySQL 초기화 스크립트가 실행되지 않을 때
```bash
# 볼륨 삭제 후 재시작
docker-compose down -v
docker-compose up -d mysql
```

#### Kafka 연결 실패 시
```bash
# Kafka 헬스체크 확인
docker exec -it ecommerce-api-kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# 토픽 목록 확인
docker exec -it ecommerce-api-kafka kafka-topics --list --bootstrap-server localhost:29092
```

#### Redis 연결 확인
```bash
docker exec -it ecommerce-api-redis redis-cli ping
# 응답: PONG
```