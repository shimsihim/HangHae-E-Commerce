package io.hhplus.tdd.domain.cart.domain.model;

import io.hhplus.tdd.common.BaseEntity;
import io.hhplus.tdd.domain.point.domain.model.TransactionType;

public class Cart {
    private Long id;
    private Long userId;
    private Long productOptionId;
    private int quantity;
}
