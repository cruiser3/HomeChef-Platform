package com.fooddelivery.service.impl;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;
import com.fooddelivery.entity.Family;
import com.fooddelivery.entity.FamilyMember;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.repository.FamilyMemberRepository;
import com.fooddelivery.repository.FamilyRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    public FamilyResponse createFamily(CreateFamilyRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Family family = Family.builder()
                .name(request.getName().trim())
                .createdBy(currentUser.getId())
                .build();
        Family saved = familyRepository.save(family);

        FamilyMember admin = FamilyMember.builder()
                .familyId(saved.getId())
                .userId(currentUser.getId())
                .role(FamilyMember.FamilyRole.ADMIN)
                .build();
        familyMemberRepository.save(admin);

        return toResponse(saved, FamilyMember.FamilyRole.ADMIN, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyResponse> listMyFamilies() {
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(currentUserService.getCurrentUserId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> familyIds = memberships.stream().map(FamilyMember::getFamilyId).toList();
        Map<Long, Family> families = familyRepository.findAllById(familyIds).stream()
                .collect(Collectors.toMap(Family::getId, family -> family));
        Map<Long, Long> memberCounts = familyMemberRepository.findAll().stream()
                .filter(member -> familyIds.contains(member.getFamilyId()))
                .collect(Collectors.groupingBy(FamilyMember::getFamilyId, Collectors.counting()));

        return memberships.stream()
                .filter(membership -> families.containsKey(membership.getFamilyId()))
                .map(membership -> toResponse(
                        families.get(membership.getFamilyId()),
                        membership.getRole(),
                        memberCounts.getOrDefault(membership.getFamilyId(), 0L).intValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyResponse getMyFamily(Long familyId) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
        int memberCount = familyMemberRepository.findByFamilyId(familyId).size();
        return toResponse(family, membership.getRole(), memberCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listMembers(Long familyId) {
        currentUserService.requireMembership(familyId);
        return buildMemberList(familyId);
    }

    private List<FamilyMemberResponse> buildMemberList(Long familyId) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);
        Map<Long, String> memberNames = userRepository.findAllById(
                members.stream().map(FamilyMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return members.stream()
                .map(member -> FamilyMemberResponse.builder()
                        .userId(member.getUserId())
                        .name(memberNames.get(member.getUserId()))
                        .role(member.getRole().name())
                        .build())
                .toList();
    }

    private FamilyResponse toResponse(Family family, FamilyMember.FamilyRole role, int memberCount) {
        return FamilyResponse.builder()
                .id(family.getId())
                .name(family.getName())
                .createdBy(family.getCreatedBy())
                .createdAt(family.getCreatedAt())
                .currentUserRole(role.name())
                .memberCount(memberCount)
                .build();
    }
}