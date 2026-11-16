package com.fooddelivery.service;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;
import com.fooddelivery.dto.InviteCodeResponse;
import com.fooddelivery.dto.JoinFamilyRequest;
import com.fooddelivery.dto.UpdateMemberRoleRequest;

import java.util.List;

public interface FamilyService {

    FamilyResponse createFamily(CreateFamilyRequest request);

    List<FamilyResponse> listMyFamilies();

    FamilyResponse getMyFamily(Long familyId);

    List<FamilyMemberResponse> listMembers(Long familyId);

    InviteCodeResponse getInviteCode(Long familyId);

    InviteCodeResponse resetInviteCode(Long familyId);

    FamilyResponse joinFamily(JoinFamilyRequest request);

    void removeMember(Long familyId, Long userId);

    FamilyMemberResponse updateMemberRole(Long familyId, Long userId, UpdateMemberRoleRequest request);
}