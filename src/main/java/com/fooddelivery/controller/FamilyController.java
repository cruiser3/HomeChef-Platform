package com.fooddelivery.controller;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;
import com.fooddelivery.dto.InviteCodeResponse;
import com.fooddelivery.dto.JoinFamilyRequest;
import com.fooddelivery.dto.UpdateMemberRoleRequest;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping
    public ResponseEntity<FamilyResponse> create(@Valid @RequestBody CreateFamilyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.createFamily(request));
    }

    @GetMapping
    public ResponseEntity<List<FamilyResponse>> listMyFamilies() {
        return ResponseEntity.ok(familyService.listMyFamilies());
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> getMyFamily(@PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.getMyFamily(familyId));
    }

    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<FamilyMemberResponse>> listMembers(@PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.listMembers(familyId));
    }

    @GetMapping("/current/members")
    public ResponseEntity<List<FamilyMemberResponse>> listCurrentFamilyMembers(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return ResponseEntity.ok(familyService.listMembers(familyId));
    }

    @GetMapping("/{familyId}/invite-code")
    public ResponseEntity<InviteCodeResponse> getInviteCode(@PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.getInviteCode(familyId));
    }

    @PostMapping("/{familyId}/invite-code/reset")
    public ResponseEntity<InviteCodeResponse> resetInviteCode(@PathVariable Long familyId) {
        return ResponseEntity.ok(familyService.resetInviteCode(familyId));
    }

    @PostMapping("/join")
    public ResponseEntity<FamilyResponse> joinFamily(@Valid @RequestBody JoinFamilyRequest request) {
        return ResponseEntity.ok(familyService.joinFamily(request));
    }

    @DeleteMapping("/{familyId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long familyId,
            @PathVariable Long userId) {
        familyService.removeMember(familyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{familyId}/members/{userId}/role")
    public ResponseEntity<FamilyMemberResponse> updateMemberRole(
            @PathVariable Long familyId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(familyService.updateMemberRole(familyId, userId, request));
    }
}