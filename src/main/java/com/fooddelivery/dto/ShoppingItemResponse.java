package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingItemResponse {
    private Long id;
    private Long familyId;
    private String name;
    private BigDecimal amount;
    private String unit;
    private Long orderId;
    private Long orderItemId;
    private Long addedBy;
    private LocalDateTime createdAt;
}