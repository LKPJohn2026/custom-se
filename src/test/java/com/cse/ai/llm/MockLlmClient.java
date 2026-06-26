package com.cse.ai.llm;

import java.util.stream.Stream;

/**
 * Deterministic LLM for unit tests.
 */
public final class MockLlmClient implements LlmClient {
	private final String answer;

	public MockLlmClient(String answer) {
		this.answer = answer;
	}

	@Override
	public String providerId() {
		return "mock";
	}

	@Override
	public String model() {
		return "mock";
	}

	@Override
	public Stream<String> streamChat(ChatRequest request) {
		return Stream.of(answer);
	}

	@Override
	public String completeChat(ChatRequest request) {
		return answer;
	}
}
