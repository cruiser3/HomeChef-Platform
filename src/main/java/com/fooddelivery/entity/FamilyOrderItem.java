package com.fooddelivery.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private FamilyOrder order;

    @Column(name = "dish_id")
    private Long dishId;

    @Column(name = "dish_name_snapshot", nullable = false, length = 100)
    private String dishNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "cook_status", nullable = false, length = 20)
    @Builder.Default
    private CookStatus cookStatus = CookStatus.PENDING;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "blocked_reason", length = 200)
    private String blockedReason;

    @Column(name = "cook_status_updated_by")
    private Long cookStatusUpdatedBy;

    @Column(name = "cook_status_updated_at")
    private LocalDateTime cookStatusUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CookStatus {
        PENDING, COOKING, READY, SERVED, BLOCKED;

        public java.util.Set<CookStatus> allowedNext() {
            return switch (this) {
                case PENDING -> java.util.EnumSet.of(COOKING, BLOCKED);
                case COOKING -> java.util.EnumSet.of(READY, BLOCKED);
                case READY -> java.util.EnumSet.of(SERVED, BLOCKED);
                case BLOCKED -> java.util.EnumSet.of(PENDING, COOKING, READY);
                case SERVED -> java.util.EnumSet.of(SERVED);
            };
        }

        public boolean canTransitionTo(CookStatus target) {
            return allowedNext().contains(target);
        }
    }
}