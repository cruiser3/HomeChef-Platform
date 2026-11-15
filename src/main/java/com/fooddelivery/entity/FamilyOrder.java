package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "family_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_orders_family_id", columnNames = {"family_id", "id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "meal_period", length = 20)
    private String mealPeriod;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "meal_served", nullable = false)
    @Builder.Default
    private boolean mealServed = false;

    @Column(name = "meal_served_at")
    private LocalDateTime mealServedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<FamilyOrderItem> items = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    public enum OrderStatus {
        CREATED, PREPARING, COMPLETED, CANCELLED
    }
}