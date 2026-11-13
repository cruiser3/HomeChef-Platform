package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}