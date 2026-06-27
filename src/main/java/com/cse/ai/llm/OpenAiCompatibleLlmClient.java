package com.cse.ai.llm;

import java.util.stream.Stream;

import com.cse.ai.http.AiHttpConfig;
import com.cse.ai.http.HttpExchange;

/**
 * OpenAI-compatible chat for local servers such as LM Studio.
 */
public final class OpenAiCompatibleLlmClient implements LlmClient {
	private final OpenAiLlmClient delegate;

	public OpenAiCompatibleLlmClient(String baseUrl, String model) {
		this(AiHttpConfig.defaults(), baseUrl, model);
	}

	public OpenAiCompatibleLlmClient(AiHttpConfig httpConfig, String baseUrl, String model) {
		this.delegate = new OpenAiLlmClient(HttpExchange.newClient(httpConfig), httpConfig,
				chatUrl(baseUrl), null, model);
	}

	@Override
	public String providerId() {
		return "openai-compatible";
	}

	@Override
	public String model() {
		return delegate.model();
	}

	@Override
	public Stream<String> streamChat(ChatRequest request) {
		return delegate.streamChat(request);
	}

	@Override
	public String completeChat(ChatRequest request) {
		return delegate.completeChat(request);
	}

	private static String chatUrl(String baseUrl) {
		String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		if (trimmed.endsWith("/chat/completions")) {
			return trimmed;
		}
		if (trimmed.endsWith("/v1")) {
			return trimmed + "/chat/completions";
		}
		return trimmed + "/v1/chat/completions";
	}
}
