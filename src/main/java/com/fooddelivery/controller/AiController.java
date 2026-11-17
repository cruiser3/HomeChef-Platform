package com.fooddelivery.controller;

import com.fooddelivery.dto.AiCalorieRequest;
import com.fooddelivery.dto.AiCalorieResponse;
import com.fooddelivery.dto.AiCartAnalyzeRequest;
import com.fooddelivery.dto.AiCartAnalyzeResponse;
import com.fooddelivery.dto.AiRecipeParseRequest;
import com.fooddelivery.dto.AiRecipeParseResponse;
import com.fooddelivery.exception.AiServiceException;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.AiCalorieService;
import com.fooddelivery.service.AiCartAnalyzeService;
import com.fooddelivery.service.AiRecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiRecipeService aiRecipeService;
    private final AiCartAnalyzeService aiCartAnalyzeService;
    private final AiCalorieService aiCalorieService;

    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;

    @PostMapping("/parse-recipe")
    public ResponseEntity<AiRecipeParseResponse> parseRecipe(@Valid @RequestBody AiRecipeParseRequest request) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return ResponseEntity.ok(aiRecipeService.parseRecipe(request.getText()));
    }

    @PostMapping("/analyze-cart")
    public ResponseEntity<AiCartAnalyzeResponse> analyzeCart(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @RequestBody(required = false) AiCartAnalyzeRequest request) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return ResponseEntity.ok(aiCartAnalyzeService.analyzeCart(familyId, request));
    }

    @PostMapping("/analyze-calories")
    public ResponseEntity<AiCalorieResponse> analyzeCalories(@Valid @RequestBody AiCalorieRequest request) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return ResponseEntity.ok(aiCalorieService.analyzeCalories(request));
    }
}
