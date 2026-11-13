package com.fooddelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCookStatusRequest {

    @NotNull(message = "Cook status is required")
    private String status;

    private String blockedReason;
}