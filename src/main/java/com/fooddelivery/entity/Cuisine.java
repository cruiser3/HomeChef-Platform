package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "cuisines",
        uniqueConstraints = @UniqueConstraint(name = "uk_cuisines_name", columnNames = {"name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuisine {

    /**
     * 固定排序值：特殊菜系「其他」的 sort_no，排在所有正常菜系（含新建）之后。
     */
    public static final int OTHER_SORT_NO = 1000;

    /**
     * 固定排序值：虚拟归类「未分类」的 sort_no，代表 cuisine_id IS NULL 的菜品，排最后。
     */
    public static final int UNCATEGORIZED_SORT_NO = 1001;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_no", nullable = false)
    @Builder.Default
    private int sortNo = 0;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
}
