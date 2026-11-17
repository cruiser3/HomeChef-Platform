package com.fooddelivery.controller;

import com.fooddelivery.dto.AiChatApiConfigRequest;
import com.fooddelivery.dto.AiChatConfigResponse;
import com.fooddelivery.dto.AiChatPersonaRequest;
import com.fooddelivery.service.AiChatConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiChatConfigController {

    private final AiChatConfigService configService;

    @GetMapping
    public ResponseEntity<AiChatConfigResponse> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    @PutMapping
    public ResponseEntity<AiChatConfigResponse> saveApiConfig(
            @RequestBody(required = false) AiChatApiConfigRequest request) {
        return ResponseEntity.ok(configService.saveApiConfig(
                request == null ? new AiChatApiConfigRequest() : request));
    }

    @PutMapping("/persona")
    public ResponseEntity<AiChatConfigResponse> savePersona(
            @RequestBody(required = false) AiChatPersonaRequest request) {
        return ResponseEntity.ok(configService.savePersona(
                request == null ? new AiChatPersonaRequest() : request));
    }

    @DeleteMapping
    public ResponseEntity<Void> resetConfig() {
        configService.resetConfig();
        return ResponseEntity.noContent().build();
    }
}
