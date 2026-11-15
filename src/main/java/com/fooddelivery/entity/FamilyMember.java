package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "family_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_member_user_family", columnNames = {"family_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "`role`", nullable = false, length = 20)
    private FamilyRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public enum FamilyRole {
        ADMIN, MEMBER
    }
}