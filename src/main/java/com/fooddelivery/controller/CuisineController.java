package com.fooddelivery.controller;

import com.fooddelivery.dto.CuisineRequest;
import com.fooddelivery.dto.CuisineResponse;
import com.fooddelivery.service.CuisineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuisines")
@RequiredArgsConstructor
public class CuisineController {

    private final CuisineService cuisineService;

    @GetMapping
    public ResponseEntity<List<CuisineResponse>> list() {
        return ResponseEntity.ok(cuisineService.list());
    }

    @PostMapping
    public ResponseEntity<CuisineResponse> create(@Valid @RequestBody CuisineRequest request) {
        return ResponseEntity.ok(cuisineService.create(request));
    }
}
