package com.fooddelivery.service;

import com.fooddelivery.entity.FamilyMember;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.FamilyMemberRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public FamilyMember requireMembership(Long familyId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, getCurrentUserId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
    }

    public void requireAdmin(Long familyId) {
        FamilyMember member = requireMembership(familyId);
        if (member.getRole() != FamilyMember.FamilyRole.ADMIN) {
            throw new ForbiddenException("Admin permission of this family is required");
        }
    }

    public FamilyMember requireExistingFamilyMembership(Long familyId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with id: " + familyId));
    }
}