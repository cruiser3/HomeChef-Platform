package com.fooddelivery.controller;

import com.fooddelivery.dto.CreateFamilyRequest;
import com.fooddelivery.dto.FamilyMemberResponse;
import com.fooddelivery.dto.FamilyResponse;
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
}