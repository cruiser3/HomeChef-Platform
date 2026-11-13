package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dish_ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "sort_no", nullable = false)
    @Builder.Default
    private int sortNo = 0;
}