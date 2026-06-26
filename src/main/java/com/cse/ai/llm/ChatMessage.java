package com.cse.ai.llm;

/**
 * One message in a chat request.
 */
public record ChatMessage(String role, String content) {
}
