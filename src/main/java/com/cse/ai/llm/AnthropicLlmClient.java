package com.cse.ai.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.cse.ai.http.AiHttpConfig;
import com.cse.ai.http.HttpExchange;

/**
 * Chat completions via Anthropic Messages API.
 */
public final class AnthropicLlmClient implements LlmClient {
	private static final String API_VERSION = "2023-06-01";

	private final HttpClient http;
	private final AiHttpConfig httpConfig;
	private final URI messagesUri;
	private final String apiKey;
	private final String model;

	public AnthropicLlmClient(String apiKey, String model) {
		this(AiHttpConfig.defaults(), apiKey, model);
	}

	public AnthropicLlmClient(AiHttpConfig httpConfig, String apiKey, String model) {
		this(HttpExchange.newClient(httpConfig), httpConfig,
				"https://api.anthropic.com/v1/messages", apiKey, model);
	}

	AnthropicLlmClient(HttpClient http, String messagesUrl, String apiKey, String model) {
		this(http, AiHttpConfig.defaults(), messagesUrl, apiKey, model);
	}

	AnthropicLlmClient(HttpClient http, AiHttpConfig httpConfig, String messagesUrl, String apiKey, String model) {
		this.http = http;
		this.httpConfig = httpConfig;
		this.messagesUri = URI.create(messagesUrl);
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public String providerId() {
		return "claude";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public Stream<String> streamChat(ChatRequest request) {
		try {
			HttpRequest httpRequest = buildRequest(request, true);
			HttpResponse<InputStream> response = HttpExchange.send(http, httpRequest,
					HttpResponse.BodyHandlers.ofInputStream(), httpConfig);
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("Claude chat returned HTTP " + response.statusCode());
			}
			return lines(response.body()).map(ChatJson::parseAnthropicDelta).filter(s -> !s.isEmpty());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("Claude chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("Claude chat request failed", e);
		}
	}

	@Override
	public String completeChat(ChatRequest request) {
		try {
			HttpRequest httpRequest = buildRequest(request, false);
			HttpResponse<String> response = HttpExchange.send(http, httpRequest,
					HttpResponse.BodyHandlers.ofString(), httpConfig);
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("Claude chat returned HTTP " + response.statusCode());
			}
			return ChatJson.parseAnthropicComplete(response.body());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("Claude chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("Claude chat request failed", e);
		}
	}

	private HttpRequest buildRequest(ChatRequest request, boolean stream) {
		String system = "";
		List<ChatMessage> messages = new ArrayList<>();
		for (ChatMessage msg : request.messages()) {
			if ("system".equals(msg.role())) {
				system = msg.content();
			} else {
				messages.add(msg);
			}
		}
		HttpRequest.Builder builder = HttpRequest.newBuilder(messagesUri)
				.header("Content-Type", "application/json")
				.header("anthropic-version", API_VERSION)
				.POST(HttpRequest.BodyPublishers.ofString(ChatJson.anthropicRequest(model, system, messages, stream,
						request.maxTokens())))
				.timeout(httpConfig.readTimeout());
		if (apiKey != null && !apiKey.isBlank()) {
			builder.header("x-api-key", apiKey);
		}
		return builder.build();
	}

	private static Stream<String> lines(InputStream in) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		return reader.lines().onClose(() -> {
			try {
				in.close();
			} catch (IOException ignored) {
			}
		});
	}
}
