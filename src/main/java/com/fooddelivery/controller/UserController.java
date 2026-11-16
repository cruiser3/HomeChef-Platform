package com.fooddelivery.controller;

import com.fooddelivery.dto.UpdateUserProfileRequest;
import com.fooddelivery.dto.UserProfileResponse;
import com.fooddelivery.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(request));
    }
}
