package com.fooddelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePurchasedRequest {

    @NotNull(message = "Purchased flag is required")
    private Boolean purchased;
}
