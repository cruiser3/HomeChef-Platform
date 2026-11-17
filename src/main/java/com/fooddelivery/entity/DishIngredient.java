package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dish_ingredients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dish_ingredients_dish_sort",
                columnNames = {"dish_id", "sort_no"}))
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

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    private String unit;

    @Column(name = "amount_text", length = 100)
    private String amountText;

    @Column(name = "sort_no", nullable = false)
    @Builder.Default
    private int sortNo = 0;
}