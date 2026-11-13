package com.fooddelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignResponsibleRequest {

    @NotNull(message = "Responsible user id is required")
    private Long responsibleUserId;
}