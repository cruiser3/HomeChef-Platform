package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dish_category_family_name", columnNames = {"family_id", "name"}),
                @UniqueConstraint(name = "uk_dish_categories_family_id", columnNames = {"family_id", "id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_no", nullable = false)
    @Builder.Default
    private int sortNo = 0;
}