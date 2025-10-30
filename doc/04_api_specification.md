# e-Commerce 주문 서비스 RESTful API 명세서


## 공통

### 공통 요청 헤더

```
Content-Type: application/json
```

### 공통 응답 구조

#### 성공 응답
```json
{
  "isSuccess": true,
  "data": { ... },
  "error": null
}
```

#### 실패 응답
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "에러 코드",
    "message": "에러 메시지",
    "details": { ... }
  }
}
```

## (유저)포인트 관리
+ ### 포인트 충전

**end-point**
```http request
POST /api/user/point/charge
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "chargeAmount": 1000
}
```

| 필드 | 타입   | 필수 | 설명        | 제약사항                                                  |
|-----|------|------|-----------|-------------------------------------------------------|
| chargeAmount | int  | O | 충전 금액     | 0 < chargeAmount <= 500,000 , 충전 후 금액 < 1,000,000,000 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
      "userId": 1,
      "point": 5000
  },
  "error": null
}
```

**Error Response (400 Bad Request)**
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


**Error Response (404 Not Found)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "U001",
    "message": "존재하지 않는 유저 식별키입니다."
  }
}
```

---

+ ### 포인트 사용

**end-point**
```http request
POST /api/user/point/use
```
**Request Headers**
```
My-User-Id: 1
```


**Request Body**
```json
{
  "useAmount": 1000
}
```

| 필드 | 타입  | 필수 | 설명    | 제약사항                                             |
|-----|-----|------|-------|--------------------------------------------------|
| useAmount | int | O | 사용 금액 | 0 < useAmount <= 보유 금액 |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
      "userId": 1,
      "point": 5000
  },
  "error": null
}
```

**Error Response (400 Bad Request)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "P002",
    "message": "사용 금액은 0보다 커야 합니다."
  }
}
```

**Error Response (400 Bad Request)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "P003",
    "message": "보유 포인트가 부족합니다."
  }
}
```

**Error Response (404 Not Found)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "U001",
    "message": "존재하지 않는 유저 식별키입니다."
  }
}
```

---
+ ### 포인트 조회

**end-point**
```http request
GET /api/user/point
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
      "userId": 1,
      "point": 5000
  },
  "error": null
}
```

**Error Response (404 Not Found)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "U001",
    "message": "존재하지 않는 유저 식별키입니다."
  }
}
```

---

+ ### 포인트 내역 조회

**end-point**
```http request
GET /api/user/pointhistory
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
      "userId" : 1,
      "pointHistory": [
        {
          "pointHistoryId": 1,
          "type": "USE",
          "amount" : 1000,
          "balanceAfter" : 5000,
          "description" : "주문 결제",
          "createdAt": "2025-10-29T10:30:00"
        },
        {...}
      ]
  },
  "error": null
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| userId | Long | 사용자 ID |
| pointHistory | Array | 포인트 이력 목록 |
| pointHistoryId | Long | 포인트 이력 ID |
| type | String | 변동 유형 (CHARGE: 충전, USE: 사용) |
| amount | Long | 변동 금액 |
| balanceAfter | Long | 변동 후 잔액 |
| description | String | 변동 사유 |
| createdAt | DateTime | 변동 일시 |

**Error Response (404 Not Found)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "U001",
    "message": "존재하지 않는 유저 식별키입니다."
  }
}
```

---

## 상품

+ ### 상품 목록 조회

**end-point**
```http request
GET /api/products
```
**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값    | 가능 값                                                                         |
|---------|------|------|------|--------|------------------------------------------------------------------------------|
| sort | String | X | 정렬 기준 | LATEST | PRICE_ASC (가격 오름차순)<br>PRICE_DESC (가격 내림차순)<br>LATEST (최신순)<br>POPULAR (인기순) |
| days | Integer | X | 인기순 조회 기간 (일) | 7      | 1,7,30 (sort=POPULAR일 때만 유효)                                                 |
| page | Integer | X | 페이지 번호 | 1      | 1 이상 , 미 기재 시 1페이지                                                           |
| size | Integer | X | 페이지 크기 | 20     | 1~100  , 미 기재 시 20개                                                          |


**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "상품명",
        "description": "상품 상세 설명",
        "basePrice": 2500000,
        "imageUrl": "https://~~~.jpg",
        "createdAt": "2025-10-20T10:00:00",
        "updatedAt": "2025-10-20T10:00:00"
      },
      {
        ...
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "error": null
}
```


+ ### 상품 상세 조회

**end-point**
```http request
GET /api/products/{productId}
```

**Path Parameters**

| 파라미터 | 타입  | 필수 | 설명 |
|---------|---------|----|----------|
| productId | Long    | O  | 상품 ID |



**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "productId": 1,
    "name": "상품명",
    "description": "상품 상세 설명",
    "basePrice": 2500000,
    "imageUrl": "https://~~~.jpg",
    "createdAt": "2025-10-20T10:00:00",
    "updatedAt": "2025-10-20T10:00:00",
    "options": [
      {
        "productOptionId": 1,
        "optionName": "상세옵션",
        "price": 2500000,
        "quantity": 50
      },
      {...}
    ]
  },
  "error": null
}
```

**Error Response (404 Not Found)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "PR001",
    "message": "존재하지 않는 상품입니다."
  }
}
```

---

## 장바구니

+ ### 장바구니 조회

**end-point**
```http request
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
    "userId": 1,
    "cartItems": [
      {
        "cartItemId": 1,
        "productId": 1,
        "productName": "상품명",
        "productOptionId": 1,
        "optionName": "옵션명",
        "price": 11000,
        "cartQuantity": 3,
        "availableStock": 50,
        "subtotal": 33000
      },
      {
        "cartItemId": 2,
        "productId": 2,
        "productName": "상품명2",
        "productOptionId": 3,
        "optionName": "옵션명2",
        "price": 25000,
        "cartQuantity": 1,
        "availableStock": 0,
        "subtotal": 25000
      }
    ],
    "totalAmount": 58000
  },
  "error": null
}
```

---
+ ### 장바구니 상품 추가

**end-point**
```http request
POST /api/cart
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "productOptions": [
    {
      "productOptionId" : 1,
      "quantity" : 10
    },{...}
  ]
}
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|-----|------|------|------|---------|
| productOptionId | Long | O | 상품 옵션 ID | - |
| quantity | Integer | O | 수량 | 1 이상 |

**Response (200 OK)**

```json
{
  "isSuccess": true,
  "data": {
    "cartItems": [
      {
        "cartItemId": 1,
        "productId": 1,
        "productName": "상품명",
        "productOptionId": 1,
        "optionName": "옵션명",
        "price": 2500000,
        "cartQuantity": 2,
        "subtotal": 5000000
      },{}
    ]
  },
  "error": null
}
```
---

+ ### 장바구니 수량 변경

**end-point**
```http request
PATCH /api/cart/{cartId}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartId | Long | O | 장바구니 항목 ID |

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "quantity": 3
}
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|-----|------|------|------|---------|
| quantity | Integer | O | 수량 | 0 이상 (0인 경우 삭제) |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "cartItemId": 1,
    "productOptionId": 1,
    "cartQuantity": 3,
    "subtotal": 7500000
  },
  "error": null
}
```

---

+ ### 장바구니 삭제

**end-point**
```http request
DELETE /api/cart/{cartId}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartId | Long | O | 장바구니 항목 ID |

**Request Headers**
```
My-User-Id: 1
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "cartItemId": 1,
    "message": "장바구니 항목이 삭제되었습니다."
  },
  "error": null
}
```
---

## 쿠폰

+ ### 쿠폰 목록 조회

**end-point**
```http request
GET /api/coupons
```

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "couponList": [
      {
        "couponId" : 1,
        "couponName" : "신규 가입 쿠폰",
        "discountType" : "PERCENTAGE",
        "discountValue" : 10,
        "minOrderValue" : 10000,
        "validFrom" : "2025-10-01T00:00:00",
        "validUntil" : "2025-12-31T23:59:59",
        "totalQuantity" : 1000,
        "issuedQuantity" : 523,
        "limitPerUser" : 1,
        "duration" : 30
      },
      {
        "couponId" : 2,
        "couponName" : "특별 할인 쿠폰",
        "discountType" : "FIXED_AMOUNT",
        "discountValue" : 5000,
        "minOrderValue" : 30000,
        "validFrom" : "2025-10-15T00:00:00",
        "validUntil" : "2025-11-15T23:59:59",
        "totalQuantity" : 500,
        "issuedQuantity" : 500,
        "limitPerUser" : 1,
        "duration" : 7
      }
    ]
  },
  "error": null
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| couponId | Long | 쿠폰 ID |
| couponName | String | 쿠폰명 |
| discountType | String | 할인 유형 (PERCENTAGE: 퍼센트 할인, FIXED_AMOUNT: 정액 할인) |
| discountValue | Integer | 할인값 (PERCENTAGE: 10=10%, FIXED_AMOUNT: 1000=1000원) |
| minOrderValue | Integer | 최소 주문 금액 |
| validFrom | DateTime | 유효기간 시작 |
| validUntil | DateTime | 유효기간 종료 |
| totalQuantity | Integer | 전체 발급 가능 수량 |
| issuedQuantity | Integer | 발급된 수량 |
| limitPerUser | Integer | 1인당 발급 제한 수량 |
| duration | Integer | 발급일로부터 유효 기간 (일) |

---

+ ### 쿠폰 발급

**end-point**
```http request
POST /api/coupons
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "couponId" : 1
}
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|-----|------|------|------|---------|
| couponId | Long | O | 쿠폰 ID | - |

**Response (200 OK)**
```json
{
  "isSuccess": true,
  "data": {
    "userCouponId": 1,
    "couponId" : 1,
    "couponName": "신규 가입 쿠폰",
    "status" : "ISSUED",
    "issuedAt" : "2025-10-29T10:30:00",
    "usedAt" : null,
    "expiresAt" : "2025-11-28T10:30:00"
  },
  "error": null
}
```
---

+ ### 사용자 보유 쿠폰 조회

**end-point**
```http request
GET /api/coupons/user
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
    "userId": 1,
    "couponList": [
      {
        "userCouponId": 1,
        "couponId" : 1,
        "couponName": "신규 가입 쿠폰",
        "discountType" : "PERCENTAGE",
        "discountValue" : 10,
        "status" : "ISSUED",
        "issuedAt" : "2025-10-29T10:30:00",
        "usedAt" : null,
        "expiresAt" : "2025-11-28T10:30:00"
      },
      {
        "userCouponId": 2,
        "couponId" : 3,
        "couponName": "만료된 쿠폰",
        "discountType" : "FIXED_AMOUNT",
        "discountValue" : 3000,
        "status" : "EXPIRED",
        "issuedAt" : "2025-09-01T10:30:00",
        "usedAt" : null,
        "expiresAt" : "2025-10-01T10:30:00"
      }
    ]
  },
  "error": null
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| userId | Long | 사용자 ID |
| userCouponId | Long | 사용자 쿠폰 ID |
| couponId | Long | 쿠폰 ID |
| couponName | String | 쿠폰명 |
| discountType | String | 할인 유형 (PERCENTAGE: 퍼센트 할인, FIXED_AMOUNT: 정액 할인) |
| discountValue | Integer | 할인값 |
| status | String | 쿠폰 상태 (ISSUED: 발급됨, USED: 사용됨, EXPIRED: 만료됨) |
| issuedAt | DateTime | 발급 일시 |
| usedAt | DateTime | 사용 일시 (nullable) |
| expiresAt | DateTime | 만료 일시 |


---
























## 주문 및 결제

+ ### 주문 생성

**end-point**
```http request
POST /api/orders
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**

```json
{
  "usePointAmount": 100000,
  "userCouponId": 1,
  "optionList": [
    {
      "productOptionId": 1,
      "quantity": 10
    },
    {
      "productOptionId": 2,
      "quantity": 5
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|-----|------|------|------|---------|
| optionList | Array | O | 주문 상품 옵션 목록 | 최소 1개 이상 |
| productOptionId | Long | O | 상품 옵션 ID | - |
| quantity | Integer | O | 주문 수량 | 1 이상 |

**Response (200 OK)**

```json
{
  "isSuccess": true,
  "data": {
    "orderId": 1,
    "userId": 1,
    "userCouponId": 1,
    "status": "PENDING",
    "totalAmount": 100000,
    "discountAmount": 0,
    "usePointAmount": 50000,
    "finalAmount": 50000,
    "createdAt": "2025-10-29T10:30:00",
    "paidAt": null,
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productOptionId": 1,
        "productName": "상품명",
        "optionName": "옵션명1",
        "quantity": 10,
        "unitPrice": 10000,
        "subtotal": 100000
      },
      {
        ...
      }
    ]
  },
  "error": null
}
```


**Error Response (400 Bad Request)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "ST001",
    "message": "재고가 부족합니다.",
    "details": {
      "productOptionId": 1,
      "requestedQuantity": 10,
      "availableQuantity": 3
    }
  }
}
```

---

+ ### 결제

**end-point**
```http request
POST /api/orders/pay
```

**Request Headers**
```
My-User-Id: 1
```

**Request Body**
```json
{
  "orderId" : 1
}
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|-----|------|------|------|---------|
| orderId | Long | O | 주문 ID | - |

**Response (200 OK)**

```json
{
  "isSuccess": true,
  "data": {
    "orderId": 1,
    "userId": 1,
    "coupon" : {
      "userCouponId": 3,
      "couponName": "쿠폰명",
      "discountAmount": 10000
    },
    "status": "PAID",
    "totalAmount": 100000,
    "discountAmount": 0,
    "usePointAmount": 50000,
    "finalAmount": 50000,
    "createdAt": "2025-10-29T10:30:00",
    "paidAt": "2025-10-29T10:30:00",
    "orderItems": [
      {
        "orderItemId": 1,
        "productId" : 1,
        "productOptionId": 1,
        "productName": "상품명",
        "optionName": "옵션명1",
        "quantity": 10,
        "unitPrice": 10000,
        "subtotal": 100000
      },
      {...}
    ]
  },
  "error": null
}
```


**Error Response (400 Bad Request)**
```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "P003",
    "message": "보유 포인트가 부족합니다."
  }
}
```

---

+ ### 주문 내역 조회

**end-point**
```http request
GET /api/orders
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
    "orders": [
      {
        "orderId": 1,
        "userId": 1,
        "userCouponId": 3,
        "status": "PAID",
        "totalAmount": 175000,
        "discountAmount": 17500,
        "finalAmount": 157500,
        "createdAt": "2025-10-29T10:30:00",
        "paidAt": "2025-10-29T10:30:00"
      },{...}
    ]
  },
  "error": null
}
```


---

+ ### 주문 상세 내역 조회

**end-point**
```http request
GET /api/orders/detail/{orderId}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| orderId | Long | O | 주문 ID |

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
    "userId": 1,
    "coupon" : {
      "userCouponId": 3,
      "couponName": "쿠폰명",
      "discountAmount": 10000
    },
    "status": "PAID",
    "totalAmount": 100000,
    "discountAmount": 10000,
    "finalAmount": 90000,
    "createdAt": "2025-10-29T10:30:00",
    "paidAt": "2025-10-29T10:30:00",
    "orderItems": [
      {
        "orderItemId": 1,
        "productId" : 1,
        "productOptionId": 1,
        "productName": "상품명",
        "optionName": "옵션명1",
        "quantity": 10,
        "unitPrice": 10000,
        "subtotal": 100000
      },{...}
    ]
  },
  "error": null
}
```