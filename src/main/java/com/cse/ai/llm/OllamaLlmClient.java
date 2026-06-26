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
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Chat completions via Ollama {@code POST /api/chat}.
 */
public final class OllamaLlmClient implements LlmClient {
	private final HttpClient http;
	private final URI chatUri;
	private final String model;

	public OllamaLlmClient(String baseUrl, String model) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), baseUrl, model);
	}

	OllamaLlmClient(HttpClient http, String baseUrl, String model) {
		this.http = http;
		String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.chatUri = URI.create(normalized + "/api/chat");
		this.model = model;
	}

	@Override
	public String providerId() {
		return "ollama";
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public Stream<String> streamChat(ChatRequest request) {
		try {
			HttpRequest httpRequest = HttpRequest.newBuilder(chatUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(
							ChatJson.ollamaRequest(model, request.messages(), true, request.temperature())))
					.timeout(Duration.ofSeconds(120))
					.build();
			HttpResponse<InputStream> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("Ollama chat returned HTTP " + response.statusCode());
			}
			return lines(response.body()).map(ChatJson::parseOllamaContent).filter(s -> !s.isEmpty());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("Ollama chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("Ollama chat request failed", e);
		}
	}

	@Override
	public String completeChat(ChatRequest request) {
		try {
			HttpRequest httpRequest = HttpRequest.newBuilder(chatUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(
							ChatJson.ollamaRequest(model, request.messages(), false, request.temperature())))
					.timeout(Duration.ofSeconds(120))
					.build();
			HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new LlmException("Ollama chat returned HTTP " + response.statusCode());
			}
			return ChatJson.parseOllamaContent(response.body());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmException("Ollama chat request failed", e);
		} catch (IOException e) {
			throw new LlmException("Ollama chat request failed", e);
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
