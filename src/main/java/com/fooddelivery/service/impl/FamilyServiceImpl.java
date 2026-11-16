package com.fooddelivery.service.impl;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;
import com.fooddelivery.dto.InviteCodeResponse;
import com.fooddelivery.dto.JoinFamilyRequest;
import com.fooddelivery.dto.UpdateMemberRoleRequest;
import com.fooddelivery.entity.Family;
import com.fooddelivery.entity.FamilyMember;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ConflictException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.FamilyMemberRepository;
import com.fooddelivery.repository.FamilyRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyServiceImpl implements FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final CurrentUserService currentUserService;

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        List<FamilyMember> memberships = familyMemberRepository
                .findByUserIdAndLeftAtIsNull(currentUserService.getCurrentUserId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> familyIds = memberships.stream().map(FamilyMember::getFamilyId).toList();
        Map<Long, Family> families = familyRepository.findAllById(familyIds).stream()
                .collect(Collectors.toMap(Family::getId, family -> family));
        Map<Long, Long> memberCounts = familyMemberRepository.findByFamilyIdInAndLeftAtIsNull(familyIds).stream()
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
                .findByFamilyIdAndUserIdAndLeftAtIsNull(familyId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
        int memberCount = familyMemberRepository.findByFamilyIdAndLeftAtIsNull(familyId).size();
        return toResponse(family, membership.getRole(), memberCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listMembers(Long familyId) {
        currentUserService.requireMembership(familyId);
        return buildMemberList(familyId);
    }

    @Override
    public InviteCodeResponse getInviteCode(Long familyId) {
        currentUserService.requireMembership(familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with id: " + familyId));
        if (family.getInviteCode() == null) {
            family.setInviteCode(generateInviteCode());
            familyRepository.save(family);
        }
        return InviteCodeResponse.builder().inviteCode(family.getInviteCode()).build();
    }

    @Override
    public InviteCodeResponse resetInviteCode(Long familyId) {
        currentUserService.requireAdmin(familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with id: " + familyId));
        family.setInviteCode(generateInviteCode());
        familyRepository.save(family);
        return InviteCodeResponse.builder().inviteCode(family.getInviteCode()).build();
    }

    @Override
    public FamilyResponse joinFamily(JoinFamilyRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        String code = request.getInviteCode().trim().toUpperCase(Locale.ROOT);

        Family family = familyRepository.findByInviteCode(code)
                .orElseThrow(() -> new BadRequestException("Invalid invite code"));

        if (familyMemberRepository.existsByFamilyIdAndUserIdAndLeftAtIsNull(family.getId(), userId)) {
            throw new ConflictException("You are already a member of this family");
        }

        FamilyMember existing = familyMemberRepository.findByFamilyIdAndUserId(family.getId(), userId)
                .orElse(null);

        FamilyMember.FamilyRole role;
        if (existing != null) {
            existing.setLeftAt(null);
            familyMemberRepository.save(existing);
            role = existing.getRole();
        } else {
            FamilyMember member = FamilyMember.builder()
                    .familyId(family.getId())
                    .userId(userId)
                    .role(FamilyMember.FamilyRole.MEMBER)
                    .build();
            familyMemberRepository.save(member);
            role = FamilyMember.FamilyRole.MEMBER;
        }

        int memberCount = familyMemberRepository.findByFamilyIdAndLeftAtIsNull(family.getId()).size();
        return toResponse(family, role, memberCount);
    }

    @Override
    public void removeMember(Long familyId, Long userId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        currentUserService.requireAdmin(familyId);

        if (currentUserId.equals(userId)) {
            throw new BadRequestException("You cannot remove yourself from the family");
        }

        FamilyMember target = familyMemberRepository
                .findByFamilyIdAndUserIdAndLeftAtIsNull(familyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this family"));

        if (target.getRole() == FamilyMember.FamilyRole.ADMIN
                && familyMemberRepository.countByFamilyIdAndRoleAndLeftAtIsNull(
                        familyId, FamilyMember.FamilyRole.ADMIN) <= 1) {
            throw new BadRequestException("Cannot remove the last admin of the family");
        }

        target.setLeftAt(LocalDateTime.now(ZoneOffset.UTC));
        familyMemberRepository.save(target);

        cartItemRepository.deleteByUserIdAndFamilyId(userId, familyId);
    }

    @Override
    public FamilyMemberResponse updateMemberRole(Long familyId, Long userId, UpdateMemberRoleRequest request) {
        currentUserService.requireAdmin(familyId);

        FamilyMember.FamilyRole newRole = parseRole(request.getRole());

        FamilyMember target = familyMemberRepository
                .findByFamilyIdAndUserIdAndLeftAtIsNull(familyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this family"));

        if (target.getRole() == FamilyMember.FamilyRole.ADMIN
                && newRole == FamilyMember.FamilyRole.MEMBER
                && familyMemberRepository.countByFamilyIdAndRoleAndLeftAtIsNull(
                        familyId, FamilyMember.FamilyRole.ADMIN) <= 1) {
            throw new BadRequestException("Cannot demote the last admin of the family");
        }

        target.setRole(newRole);
        FamilyMember saved = familyMemberRepository.save(target);

        String name = userRepository.findById(saved.getUserId()).map(User::getName).orElse(null);
        return FamilyMemberResponse.builder()
                .userId(saved.getUserId())
                .name(name)
                .role(saved.getRole().name())
                .build();
    }

    private FamilyMember.FamilyRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("Role is required");
        }
        try {
            return FamilyMember.FamilyRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private List<FamilyMemberResponse> buildMemberList(Long familyId) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyIdAndLeftAtIsNull(familyId);
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