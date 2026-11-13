package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyOrderResponse {
    private Long id;
    private Long familyId;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String status;
    private String mealPeriod;
    private String remark;
    private boolean mealServed;
    private LocalDateTime mealServedAt;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FamilyOrderItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FamilyOrderItemResponse {
        private Long id;
        private Long dishId;
        private String dishName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private String cookStatus;
        private Long responsibleUserId;
        private String responsibleUserName;
        private String blockedReason;
        private Long cookStatusUpdatedBy;
        private LocalDateTime cookStatusUpdatedAt;
    }
}