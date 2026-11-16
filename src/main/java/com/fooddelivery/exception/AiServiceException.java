package com.fooddelivery.exception;

/**
 * AI 服务调用失败（上游 Ollama 不可用、返回非 JSON、超时等）时抛出的业务异常。
 * 由 {@link GlobalExceptionHandler} 统一处理为 502 Bad Gateway。
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }
}
