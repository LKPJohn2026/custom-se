package com.cse.ai.llm;

import java.util.List;

/**
 * One chat completion request.
 */
public record ChatRequest(
		List<ChatMessage> messages,
		double temperature,
		int maxTokens) {

	public static ChatRequest of(String system, String user) {
		return new ChatRequest(List.of(
				new ChatMessage("system", system),
				new ChatMessage("user", user)), 0.2, 1024);
	}
}
