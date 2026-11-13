package com.fooddelivery.service;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;

import java.util.List;

public interface FamilyService {

    FamilyResponse createFamily(CreateFamilyRequest request);

    List<FamilyResponse> listMyFamilies();

    FamilyResponse getMyFamily(Long familyId);

    List<FamilyMemberResponse> listMembers(Long familyId);
}