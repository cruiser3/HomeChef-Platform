package com.fooddelivery.service;

import com.fooddelivery.dto.UpdateUserProfileRequest;
import com.fooddelivery.dto.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateUserProfileRequest request);
}
