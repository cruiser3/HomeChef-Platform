package com.fooddelivery.service;

import com.fooddelivery.dto.AiChatMessageDto;
import com.fooddelivery.dto.AiChatSessionCreateRequest;
import com.fooddelivery.dto.AiChatSessionDto;
import com.fooddelivery.entity.AiChatSession;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.AiChatMessageRepository;
import com.fooddelivery.repository.AiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 「小铲」会话管理：当前用户的多会话列表、新建、历史查询、删除。
 * 会话归属用户（健康档案为用户级），越权访问一律拒绝。
 */
@Service
@RequiredArgsConstructor
public class AiChatSessionService {

    private static final int TITLE_MAX = 20;

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final CurrentUserService currentUserService;

    public List<AiChatSessionDto> listSessions() {
        Long userId = currentUserService.getCurrentUserId();
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public AiChatSessionDto createSession(AiChatSessionCreateRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        String title = (request == null || request.getTitle() == null || request.getTitle().isBlank())
                ? "新对话" : request.getTitle().trim();
        if (title.length() > TITLE_MAX) {
            title = title.substring(0, TITLE_MAX);
        }
        return toDto(sessionRepository.save(AiChatSession.builder()
                .userId(userId)
                .title(title)
                .build()));
    }

    public List<AiChatMessageDto> getMessages(Long sessionId) {
        Long userId = currentUserService.getCurrentUserId();
        requireOwnedSession(sessionId, userId);
        return messageRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .map(m -> new AiChatMessageDto(m.getRole(), m.getContent()))
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        Long userId = currentUserService.getCurrentUserId();
        requireOwnedSession(sessionId, userId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private AiChatSession requireOwnedSession(Long sessionId, Long userId) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("无权访问该会话");
        }
        return session;
    }

    private AiChatSessionDto toDto(AiChatSession s) {
        return AiChatSessionDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
