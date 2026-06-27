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
import java.util.stream.Stream;

import com.cse.ai.http.AiHttpConfig;
import com.cse.ai.http.HttpExchange;

/**
 * Chat completions via OpenAI {@code POST /v1/chat/completions}.
 */
public class OpenAiLlmClient implements LlmClient {
	private final HttpClient http;
	private final AiHttpConfig httpConfig;
	private final URI chatUri;
	private final String apiKey;
	private final String model;

	public OpenAiLlmClient(String apiKey, String model) {
		this(AiHttpConfig.defaults(), apiKey, model);
	}

	public OpenAiLlmClient(AiHttpConfig httpConfig, String apiKey, String model) {
		this(HttpExchange.newClient(httpConfig), httpConfig,
				"https://api.openai.com/v1/chat/completions", apiKey, model);
	}

	OpenAiLlmClient(HttpClient http, String chatUrl, String apiKey, String model) {
		this(http, AiHttpConfig.defaults(), chatUrl, apiKey, model);
	}

	OpenAiLlmClient(HttpClient http, AiHttpConfig httpConfig, String chatUrl, String apiKey, String model) {
		this.http = http;
		this.httpConfig = httpConfig;
		this.chatUri = URI.create(chatUrl);
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public String providerId() {
		return "openai";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public Stream<String> streamChat(ChatRequest request) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(chatUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(ChatJson.openAiRequest(model, request.messages(),
							true, request.temperature(), request.maxTokens())))
					.timeout(httpConfig.readTimeout());
			if (apiKey != null && !apiKey.isBlank()) {
				builder.header("Authorization", "Bearer " + apiKey);
			}
			HttpResponse<InputStream> response = HttpExchange.send(http, builder.build(),
					HttpResponse.BodyHandlers.ofInputStream(), httpConfig);
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("OpenAI chat returned HTTP " + response.statusCode());
			}
			return lines(response.body()).map(ChatJson::parseOpenAiDelta).filter(s -> !s.isEmpty());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("OpenAI chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("OpenAI chat request failed", e);
		}
	}

	@Override
	public String completeChat(ChatRequest request) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(chatUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(ChatJson.openAiRequest(model, request.messages(),
							false, request.temperature(), request.maxTokens())))
					.timeout(httpConfig.readTimeout());
			if (apiKey != null && !apiKey.isBlank()) {
				builder.header("Authorization", "Bearer " + apiKey);
			}
			HttpResponse<String> response = HttpExchange.send(http, builder.build(),
					HttpResponse.BodyHandlers.ofString(), httpConfig);
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("OpenAI chat returned HTTP " + response.statusCode());
			}
			return ChatJson.parseOpenAiComplete(response.body());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("OpenAI chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("OpenAI chat request failed", e);
		}
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
