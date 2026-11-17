package com.fooddelivery.controller;

import com.fooddelivery.dto.AiCalorieRequest;
import com.fooddelivery.dto.AiCalorieResponse;
import com.fooddelivery.dto.AiChatMessageDto;
import com.fooddelivery.dto.AiChatRequest;
import com.fooddelivery.dto.AiChatSessionCreateRequest;
import com.fooddelivery.dto.AiChatSessionDto;
import com.fooddelivery.dto.AiCartAnalyzeRequest;
import com.fooddelivery.dto.AiCartAnalyzeResponse;
import com.fooddelivery.dto.AiRecipeParseRequest;
import com.fooddelivery.dto.AiRecipeParseResponse;
import com.fooddelivery.exception.AiServiceException;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.AiCalorieService;
import com.fooddelivery.service.AiChatService;
import com.fooddelivery.service.AiChatSessionService;
import com.fooddelivery.service.AiCartAnalyzeService;
import com.fooddelivery.service.AiRecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiRecipeService aiRecipeService;
    private final AiCartAnalyzeService aiCartAnalyzeService;
    private final AiCalorieService aiCalorieService;
    private final AiChatService aiChatService;
    private final AiChatSessionService aiChatSessionService;

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

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody(required = false) AiChatRequest request) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return aiChatService.chat(request);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AiChatSessionDto>> listSessions() {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return ResponseEntity.ok(aiChatSessionService.listSessions());
    }

    @PostMapping("/sessions")
    public ResponseEntity<AiChatSessionDto> createSession(@RequestBody(required = false) AiChatSessionCreateRequest request) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return ResponseEntity.ok(aiChatSessionService.createSession(request));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<AiChatMessageDto>> getSessionMessages(@PathVariable Long id) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        return ResponseEntity.ok(aiChatSessionService.getMessages(id));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        if (!aiEnabled) {
            throw new AiServiceException("AI 功能未启用");
        }
        aiChatSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
