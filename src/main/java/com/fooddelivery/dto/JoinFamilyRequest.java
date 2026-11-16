package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinFamilyRequest {

    @NotBlank(message = "Invite code is required")
    @Size(max = 16, message = "Invite code must not exceed 16 characters")
    private String inviteCode;
}
