package io.hhplus.tdd.domain.product.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;

    @OneToMany(mappedBy = "product")
    private List<ProductOption> options = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Long basePrice;

}
