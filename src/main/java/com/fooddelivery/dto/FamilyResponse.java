package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyResponse {
    private Long id;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private String currentUserRole;
    private int memberCount;
}