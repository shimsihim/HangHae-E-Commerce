# e-Commerce 주문 서비스 RESTful API 명세서

## 목차
1. [공통 사항](#공통-사항)
2. [인증](#인증)
3. [유저 및 포인트](#유저-및-포인트)
4. [상품](#상품)
5. [장바구니](#장바구니)
6. [쿠폰](#쿠폰)
7. [주문 및 결제](#주문-및-결제)
8. [에러 코드](#에러-코드)

---

## 공통 사항

### Base URL
```
http://localhost:8080
```

### 공통 요청 헤더

```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}  # 인증이 필요한 API의 경우
```

> **참고**: 학습용 프로젝트이므로 간소화된 인증 방식으로 `My-User-Id` 헤더 사용 가능

### 공통 응답 구조

#### 성공 응답 (200 OK)
```json
{
  "isSuccess": true,
  "data": { ... },
  "error": null
}
```

#### 실패 응답 (4xx, 5xx)

```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "P001",
    "message": "충전 금액은 0보다 커야 합니다."
  }
}
```

### HTTP 상태 코드

| 상태 코드 | 설명                  |
|-------|---------------------|
| 200   | 성공                  |
| 201   | 생성 성공               |
| 400   | 잘못된 요청 (유효성 검증 실패)  |
| 401   | 인증 실패               |
| 403   | 권한 없음               |
| 404   | 리소스를 찾을 수 없음        |
| 409   | 충돌 (중복, 재고 부족 등)    |
| 500   | 서버 내부 오류            |

---

## 인증

### 로그인 (간소화 버전)

> 학습용 프로젝트이므로 실제 로그인 API는 생략하고, 테스트용 `My-User-Id` 헤더 사용

**실제 구현 시 권장사항:**
```
POST /api/auth/login
Request Body: { "email": "user@example.com", "password": "password" }
Response: { "accessToken": "JWT_TOKEN", "refreshToken": "..." }
```

---

## 유저 및 포인트

### 1. 포인트 충전

포인트를 충전합니다.

**Endpoint**
```http
POST /api/users/point/charge
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "amount": 100000
}
```

| 필드     | 타입      | 필수 | 설명   | 제약사항                                          |
|--------|---------|----|----- |-----------------------------------------------|
| amount | Long    | O  | 충전 금액 | 0 < amount <= 500,000<br>충전 후 총액 < 1,000,000,000 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "balance": 150000,
    "amount": 100000
  },
  "error": null
}
```

**Error Responses**

| HTTP | Code  | Message                |
|------|-------|------------------------|
| 400  | P001  | 충전 금액은 0보다 커야 합니다.      |
| 400  | P002  | 1회 충전 금액은 500,000원을 초과할 수 없습니다. |
| 400  | P003  | 충전 후 총액이 1,000,000,000원을 초과할 수 없습니다. |
| 404  | U001  | 존재하지 않는 사용자입니다.        |


---

### 2. 포인트 조회

현재 보유 포인트를 조회합니다.

**Endpoint**
```http
GET /api/users/point
```

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "balance": 150000
  },
  "error": null
}
```

---

### 3. 포인트 이력 조회

포인트 충전 및 사용 이력을 조회합니다.

**Endpoint**
```http
GET /api/users/point/history
```

**Request Headers**
```
My-User-Id: 1
```

**Query Parameters**

| 파라미터 | 타입      | 필수 | 설명       | 기본값 |
|------|---------|----|-----------|----|
| page | Integer | X  | 페이지 번호    | 1  |
| size | Integer | X  | 페이지 크기    | 20 |
| type | String  | X  | 필터 (CHARGE, USE) | 전체 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "history": [
      {
        "type": "USE",
        "amount": -50000,
        "balanceAfter": 150000,
        "description": "주문 결제",
        "createdAt": "2025-01-15T14:30:00"
      },
      {
        "type": "CHARGE",
        "amount": 100000,
        "balanceAfter": 200000,
        "description": "포인트 충전",
        "createdAt": "2025-01-15T10:00:00"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 15,
      "hasNext": false
    }
  },
  "error": null
}
```

> **참고**: `amount`는 충전 시 양수(+), 사용 시 음수(-)로 표현하여 UI에서 직관적으로 사용 가능

---

## 상품

### 1. 상품 목록 조회

상품 목록을 조회합니다. (페이징, 정렬 지원)

**Endpoint**
```http
GET /api/products
```

**Query Parameters**

| 파라미터 | 타입      | 필수 | 설명         | 기본값    | 가능 값                                           |
|------|---------|----|-----------|---------|-------------------------------------------------|
| sort | String  | X  | 정렬 기준     | LATEST  | PRICE_ASC, PRICE_DESC, LATEST, POPULAR          |
| days | Integer | X  | 인기순 조회 기간 | 7       | 1, 7, 30 (sort=POPULAR일 때만 유효)                 |
| page | Integer | X  | 페이지 번호    | 1       | 1 이상                                            |
| size | Integer | X  | 페이지 크기    | 20      | 1~100                                           |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "products": [
      {
        "productId": 1,
        "name": "Apple 맥북 프로 14인치",
        "basePrice": 2790000,
        "imageUrl": "https://example.com/images/macbook.jpg",
        "salesCount": 150
      },
      {
        "productId": 2,
        "name": "아이폰 15 Pro",
        "basePrice": 1550000,
        "imageUrl": "https://example.com/images/iphone.jpg",
        "salesCount": 320
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 100,
      "hasNext": true
    }
  },
  "error": null
}
```

> **참고**:
> - 목록 조회이므로 `description`, `createdAt`, `updatedAt` 등 불필요한 정보 제거
> - `salesCount`는 `sort=POPULAR`일 때만 포함
> - `basePrice`는 기본 가격 (옵션별 가격은 상세 조회에서 제공)

---

### 2. 상품 상세 조회

특정 상품의 상세 정보와 옵션을 조회합니다.

**Endpoint**
```http
GET /api/products/{productId}
```

**Path Parameters**

| 파라미터       | 타입   | 필수 | 설명   |
|------------|------|----|------|
| product_id | Long | O  | 상품 ID |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "productId": 1,
    "name": "Apple 맥북 프로 14인치",
    "description": "M3 Pro 칩 탑재 고성능 노트북",
    "imageUrl": "https://example.com/images/macbook.jpg",
    "options": [
      {
        "productOptionId": 1,
        "optionName": "스페이스 그레이 / 512GB",
        "price": 2790000,
        "quantity": 50,
        "isAvailable": true
      },
      {
        "productOptionId": 2,
        "optionName": "실버 / 1TB",
        "price": 3290000,
        "quantity": 0,
        "isAvailable": false
      }
    ]
  },
  "error": null
}
```

> **참고**: `createdAt`, `updatedAt`, `basePrice` 등 프론트엔드에서 사용하지 않는 정보 제거

**Error Responses**

| HTTP | Code   | Message        |
|------|--------|----------------|
| 404  | PR001  | 존재하지 않는 상품입니다. |

---

### 3. 상품 재고 조회

특정 상품 옵션의 실시간 재고를 조회합니다.

**Endpoint**
```http
GET /api/products/options/{productOptionId}/stock
```

**Path Parameters**

| 파라미터                | 타입   | 필수 | 설명       |
|---------------------|------|----|----------|
| product_option_id | Long | O  | 상품 옵션 ID |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "quantity": 50,
    "isAvailable": true
  },
  "error": null
}
```

> **참고**: 재고 조회는 수량 확인이 목적이므로 상품 정보는 제외 (클라이언트가 이미 알고 있음)

---

## 장바구니

### 1. 장바구니 조회

사용자의 장바구니 목록을 조회합니다.

**Endpoint**
```http
GET /api/cart
```

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "items": [
      {
        "cartId": 1,
        "productId": 1,
        "productName": "Apple 맥북 프로 14인치",
        "productOptionId": 1,
        "optionName": "스페이스 그레이 / 512GB",
        "imageUrl": "https://example.com/images/macbook.jpg",
        "price": 2790000,
        "quantity": 2,
        "subtotal": 5580000,
        "stockQuantity": 50,
        "isAvailable": true
      },
      {
        "cartId": 2,
        "productId": 2,
        "productName": "아이폰 15 Pro",
        "productOptionId": 5,
        "optionName": "티타늄 블랙 / 256GB",
        "imageUrl": "https://example.com/images/iphone.jpg",
        "price": 1550000,
        "quantity": 1,
        "subtotal": 1550000,
        "stockQuantity": 0,
        "isAvailable": false
      }
    ],
    "summary": {
      "totalAmount": 7130000,
      "totalItems": 2,
      "availableItems": 1,
      "unavailableItems": 1
    }
  },
  "error": null
}
```

> **참고**:
> - `imageUrl` 추가하여 장바구니 UI에서 이미지 표시 가능
> - `createdAt` 제거 (불필요)
> - `summary` 객체로 요약 정보 그룹화

---

### 2. 장바구니 상품 추가

장바구니에 상품을 추가합니다. (이미 존재하는 옵션은 수량 증가)

**Endpoint**
```http
POST /api/cart
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "items": [
    {
      "productOptionId": 1,
      "quantity": 2
    },
    {
      "productOptionId": 3,
      "quantity": 1
    }
  ]
}
```

| 필드              | 타입      | 필수 | 설명       | 제약사항  |
|-----------------|---------|----|-----------|----- |
| items           | Array   | O  | 추가할 상품 목록 | 최소 1개 이상 |
| productOptionId | Long    | O  | 상품 옵션 ID  | -    |
| quantity        | Integer | O  | 수량        | 1 이상 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "addedCount": 2,
    "totalCartItems": 3
  },
  "error": null
}
```

> **참고**: 장바구니 추가 후 전체 장바구니는 `GET /api/cart`로 다시 조회하는 것을 권장

**Error Responses**

| HTTP | Code   | Message           |
|------|--------|-------------------|
| 404  | PR002  | 존재하지 않는 상품 옵션입니다. |


---

### 3. 장바구니 수량 변경

장바구니 항목의 수량을 변경합니다.

**Endpoint**
```http
PATCH /api/cart/{cartId}
```

**Path Parameters**

| 파라미터    | 타입   | 필수 | 설명        |
|---------|------|----|-----------|
| cart_id | Long | O  | 장바구니 항목 ID |

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "quantity": 5
}
```

| 필드       | 타입      | 필수 | 설명 | 제약사항            |
|----------|---------|----|----|-----------------|
| quantity | Integer | O  | 수량 | 1 이상 (0인 경우 삭제) |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "quantity": 5,
    "subtotal": 13950000
  },
  "error": null
}
```

**Error Responses**

| HTTP | Code   | Message            |
|------|--------|--------------------|
| 404  | C001   | 존재하지 않는 장바구니 항목입니다. |
| 403  | C002   | 다른 사용자의 장바구니입니다.   |

---

### 4. 장바구니 삭제

장바구니에서 항목을 삭제합니다.

**Endpoint**
```http
DELETE /api/cart/{cartId}
```

**Path Parameters**

| 파라미터    | 타입   | 필수 | 설명        |
|---------|------|----|-----------|
| cart_id | Long | O  | 장바구니 항목 ID |

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "deleted": true
  },
  "error": null
}
```

---

## 쿠폰

### 1. 발급 가능한 쿠폰 목록 조회

현재 발급 가능한 쿠폰 목록을 조회합니다.

**Endpoint**
```http
GET /api/coupons
```

**Query Parameters**

| 파라미터      | 타입     | 필수 | 설명      | 기본값   | 가능 값                    |
|-----------|--------|----|---------|-------|------------------------|
| status    | String | X  | 쿠폰 상태  | ACTIVE | ACTIVE, UPCOMING, EXPIRED |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "coupons": [
      {
        "couponId": 1,
        "couponName": "신규 가입 10% 할인 쿠폰",
        "discountType": "PERCENTAGE",
        "discountValue": 10,
        "minOrderValue": 50000,
        "remainingQuantity": 477,
        "validUntil": "2025-12-31T23:59:59",
        "isAvailable": true
      },
      {
        "couponId": 2,
        "couponName": "5,000원 즉시 할인",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 5000,
        "minOrderValue": 30000,
        "remainingQuantity": 0,
        "validUntil": "2025-01-31T23:59:59",
        "isAvailable": false
      }
    ]
  },
  "error": null
}
```

> **참고**:
> - `totalQuantity`, `issuedQuantity`, `limitPerUser`, `duration`, `validFrom`, `createdAt` 제거
> - 프론트엔드에서 필요한 정보만 포함: 이름, 할인 정보, 최소 주문금액, 남은 수량, 만료일

---

### 2. 쿠폰 발급 (선착순) 🔥

선착순으로 쿠폰을 발급받습니다.

> **동시성 제어**: Redis 분산 락 + Lua 스크립트 사용

**Endpoint**
```http
POST /api/coupons/{couponId}/issue
```

**Path Parameters**

| 파라미터      | 타입   | 필수 | 설명   |
|-----------|------|----|------|
| coupon_id | Long | O  | 쿠폰 ID |

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "userCouponId": 1,
    "couponName": "신규 가입 10% 할인 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "minOrderValue": 50000,
    "expiresAt": "2025-02-14T14:30:00"
  },
  "error": null
}
```

> **참고**: `couponId`, `status`, `issuedAt` 제거하여 응답 간소화

**Error Responses**

| HTTP | Code   | Message                |
|------|--------|------------------------|
| 404  | CP001  | 존재하지 않는 쿠폰입니다.         |
| 409  | CP002  | 쿠폰이 모두 소진되었습니다.       |
| 409  | CP003  | 이미 발급받은 쿠폰입니다.        |
| 409  | CP004  | 발급 가능 수량을 초과했습니다.     |
| 400  | CP005  | 쿠폰 발급 기간이 아닙니다.       |


---

### 3. 보유 쿠폰 조회

사용자가 보유한 쿠폰 목록을 조회합니다.

**Endpoint**
```http
GET /api/coupons/my
```

**Request Headers**
```
My-User-Id: 1
```

**Query Parameters**

| 파라미터   | 타입     | 필수 | 설명       | 기본값 | 가능 값                  |
|--------|--------|----|----------|-----|----------------------|
| status | String | X  | 쿠폰 상태 필터 | 전체  | ISSUED, USED, EXPIRED |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "coupons": [
      {
        "userCouponId": 1,
        "couponName": "신규 가입 10% 할인 쿠폰",
        "discountType": "PERCENTAGE",
        "discountValue": 10,
        "minOrderValue": 50000,
        "status": "ISSUED",
        "expiresAt": "2025-02-14T14:30:00"
      },
      {
        "userCouponId": 2,
        "couponName": "만료된 쿠폰",
        "discountType": "FIXED_AMOUNT",
        "discountValue": 3000,
        "minOrderValue": 10000,
        "status": "EXPIRED",
        "expiresAt": "2024-12-31T23:59:59"
      }
    ],
    "summary": {
      "total": 2,
      "issued": 1,
      "used": 0,
      "expired": 1
    }
  },
  "error": null
}
```

> **참고**:
> - `couponId`, `issuedAt`, `usedAt` 제거
> - `status`는 DB의 enum 값 그대로 사용 (`ISSUED`, `USED`, `EXPIRED`)
> - 통계 정보는 `summary` 객체로 그룹화

---

## 주문 및 결제

### 1. 주문 생성 및 결제

주문을 생성하고 즉시 결제합니다.

> **트랜잭션**: 주문 생성 → 재고 차감 → 포인트 차감 → 쿠폰 사용 처리
> **동시성 제어**: 재고 차감 시 비관적 락, 포인트 차감 시 낙관적 락

**Endpoint**
```http
POST /api/orders
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "items": [
    {
      "productOptionId": 1,
      "quantity": 2
    },
    {
      "productOptionId": 3,
      "quantity": 1
    }
  ],
  "userCouponId": 1,
  "usePointAmount": 0
}
```

| 필드              | 타입      | 필수 | 설명         | 제약사항       |
|-----------------|---------|----|-----------|-----------  |
| items           | Array   | O  | 주문 상품 목록  | 최소 1개 이상   |
| productOptionId | Long    | O  | 상품 옵션 ID  | -          |
| quantity        | Integer | O  | 주문 수량     | 1 이상       |
| userCouponId    | Long    | X  | 사용할 쿠폰 ID | nullable   |
| usePointAmount  | Long    | X  | 사용할 포인트   | 0 이상, 기본값 0 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "orderId": 1,
    "status": "PAID",
    "totalAmount": 5580000,
    "discountAmount": 558000,
    "usePointAmount": 0,
    "finalAmount": 5022000,
    "coupon": {
      "couponName": "신규 가입 10% 할인 쿠폰",
      "discountAmount": 558000
    },
    "items": [
      {
        "productId": 1,
        "productName": "Apple 맥북 프로 14인치",
        "optionName": "스페이스 그레이 / 512GB",
        "imageUrl": "https://example.com/images/macbook.jpg",
        "quantity": 2,
        "unitPrice": 2790000,
        "subtotal": 5580000
      }
    ],
    "createdAt": "2025-01-15T14:30:00"
  },
  "error": null
}
```

> **참고**:
> - `userId`, `orderItemId`, `productOptionId`, `paidAt`, `userCouponId` 제거
> - DB 컬럼명을 camelCase로 변환: `totalAmount`, `discountAmount`, `usePointAmount`, `finalAmount`
> - `imageUrl` 추가하여 주문 내역에서 상품 이미지 표시 가능

**Error Responses**

| HTTP | Code   | Message                   |
|------|--------|---------------------------|
| 400  | ST001  | 재고가 부족합니다.                |
| 400  | P004   | 포인트가 부족합니다.               |
| 400  | CP006  | 사용할 수 없는 쿠폰입니다.           |
| 400  | CP007  | 최소 주문 금액을 충족하지 않습니다.      |
| 404  | PR002  | 존재하지 않는 상품 옵션입니다.         |
| 400  | E001   | 잘못된 요청입니다. (유효성 검증 실패)   |


---

### 2. 주문 내역 조회

사용자의 주문 내역을 조회합니다.

**Endpoint**
```http
GET /api/orders
```

**Request Headers**
```
My-User-Id: 1
```

**Query Parameters**

| 파라미터   | 타입      | 필수 | 설명       | 기본값 | 가능 값                  |
|--------|---------|----|-----------|----|----------------------|
| status | String  | X  | 주문 상태 필터 | 전체 | PENDING, PAID, CANCELLED |
| page   | Integer | X  | 페이지 번호   | 1  | 1 이상                 |
| size   | Integer | X  | 페이지 크기   | 20 | 1~100                |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "orders": [
      {
        "orderId": 3,
        "status": "PAID",
        "finalAmount": 5022000,
        "itemCount": 2,
        "createdAt": "2025-01-15T14:30:00"
      },
      {
        "orderId": 2,
        "status": "PAID",
        "finalAmount": 1500000,
        "itemCount": 1,
        "createdAt": "2025-01-14T10:00:00"
      },
      {
        "orderId": 1,
        "status": "CANCELLED",
        "finalAmount": 2790000,
        "itemCount": 1,
        "createdAt": "2025-01-13T15:20:00"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 3,
      "hasNext": false
    }
  },
  "error": null
}
```

> **참고**:
> - 목록 조회이므로 `totalAmount`, `discountAmount`, `usePointAmount`, `paidAt` 제거
> - 최종 결제 금액(`finalAmount`)과 주문 개수만 표시
> - 상세 정보는 주문 상세 조회 API 사용

---

### 3. 주문 상세 조회

특정 주문의 상세 정보를 조회합니다.

**Endpoint**
```http
GET /api/orders/{orderId}
```

**Path Parameters**

| 파라미터     | 타입   | 필수 | 설명   |
|----------|------|----|------|
| order_id | Long | O  | 주문 ID |

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "orderId": 1,
    "status": "PAID",
    "totalAmount": 5580000,
    "discountAmount": 558000,
    "usePointAmount": 0,
    "finalAmount": 5022000,
    "coupon": {
      "couponName": "신규 가입 10% 할인 쿠폰",
      "discountType": "PERCENTAGE",
      "discountValue": 10,
      "discountAmount": 558000
    },
    "items": [
      {
        "productId": 1,
        "productName": "Apple 맥북 프로 14인치",
        "optionName": "스페이스 그레이 / 512GB",
        "imageUrl": "https://example.com/images/macbook.jpg",
        "quantity": 2,
        "unitPrice": 2790000,
        "subtotal": 5580000
      }
    ],
    "createdAt": "2025-01-15T14:30:00"
  },
  "error": null
}
```

> **참고**:
> - `userId`, `orderItemId`, `productOptionId`, `paidAt`, `userCouponId` 제거
> - DB 컬럼명을 camelCase로 변환하여 일관성 유지
> - 주문 생성 응답과 동일한 구조 유지

**Error Responses**

| HTTP | Code   | Message         |
|------|--------|-----------------|
| 404  | O001   | 존재하지 않는 주문입니다.  |
| 403  | O002   | 다른 사용자의 주문입니다. |

---

## 에러 코드

### 공통 에러

| Code  | Message              |
|-------|----------------------|
| E000  | 서버 내부 오류가 발생했습니다.   |
| E001  | 잘못된 요청입니다.           |
| E002  | 필수 파라미터가 누락되었습니다.   |
| E003  | 유효하지 않은 파라미터 값입니다.  |

### 사용자 (U)

| Code  | Message          |
|-------|------------------|
| U001  | 존재하지 않는 사용자입니다.  |
| U002  | 인증되지 않은 사용자입니다.  |

### 포인트 (P)

| Code  | Message                         |
|-------|---------------------------------|
| P001  | 충전 금액은 0보다 커야 합니다.               |
| P002  | 1회 충전 금액은 500,000원을 초과할 수 없습니다.  |
| P003  | 충전 후 총액이 1,000,000,000원을 초과할 수 없습니다. |
| P004  | 포인트가 부족합니다.                     |

### 상품 (PR)

| Code  | Message           |
|-------|-------------------|
| PR001 | 존재하지 않는 상품입니다.    |
| PR002 | 존재하지 않는 상품 옵션입니다. |

### 재고 (ST)

| Code  | Message      |
|-------|--------------|
| ST001 | 재고가 부족합니다.   |

### 장바구니 (C)

| Code  | Message              |
|-------|----------------------|
| C001  | 존재하지 않는 장바구니 항목입니다. |
| C002  | 다른 사용자의 장바구니입니다.   |

### 쿠폰 (CP)

| Code  | Message             |
|-------|---------------------|
| CP001 | 존재하지 않는 쿠폰입니다.     |
| CP002 | 쿠폰이 모두 소진되었습니다.    |
| CP003 | 이미 발급받은 쿠폰입니다.     |
| CP004 | 발급 가능 수량을 초과했습니다.  |
| CP005 | 쿠폰 발급 기간이 아닙니다.    |
| CP006 | 사용할 수 없는 쿠폰입니다.    |
| CP007 | 최소 주문 금액을 충족하지 않습니다. |

### 주문 (O)

| Code  | Message          |
|-------|------------------|
| O001  | 존재하지 않는 주문입니다.   |
| O002  | 다른 사용자의 주문입니다.  |
| O003  | 이미 결제된 주문입니다.   |
| O004  | 취소할 수 없는 주문입니다. |

---

## API 사용 시나리오

### 일반적인 주문 플로우

```
1. 상품 목록 조회
   GET /api/products

2. 상품 상세 조회
   GET /api/products/1

3. 장바구니 추가
   POST /api/cart

4. 장바구니 조회
   GET /api/cart

5. 쿠폰 발급
   POST /api/coupons/1/issue

6. 보유 쿠폰 조회
   GET /api/coupons/my

7. 주문 생성 및 결제
   POST /api/orders

8. 주문 상세 조회
   GET /api/orders/1
```

### 선착순 쿠폰 발급 플로우

```
1. 발급 가능한 쿠폰 목록 조회
   GET /api/coupons

2. 쿠폰 발급 (선착순)
   POST /api/coupons/1/issue
   → Redis 분산 락 + Lua 스크립트로 동시성 제어

3. 보유 쿠폰 확인
   GET /api/coupons/my
```
