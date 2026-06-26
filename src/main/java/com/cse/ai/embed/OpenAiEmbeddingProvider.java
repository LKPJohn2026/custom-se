package com.cse.ai.embed;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Embeddings via OpenAI {@code POST /v1/embeddings}.
 */
public final class OpenAiEmbeddingProvider implements EmbeddingProvider {
	private final HttpClient http;
	private final URI embedUri;
	private final String apiKey;
	private final String model;
	private final int dimensions;

	public OpenAiEmbeddingProvider(String apiKey, String model, int dimensions) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
				"https://api.openai.com/v1/embeddings", apiKey, model, dimensions);
	}

	OpenAiEmbeddingProvider(HttpClient http, String embeddingsUrl, String apiKey, String model, int dimensions) {
		this.http = http;
		this.embedUri = URI.create(embeddingsUrl);
		this.apiKey = apiKey;
		this.model = model;
		this.dimensions = dimensions;
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
	public int dimensions() {
		return dimensions;
	}

	@Override
	public float[] embed(String text) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(embedUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(EmbeddingJson.openAiRequest(model, text)))
					.timeout(Duration.ofSeconds(60));
			if (apiKey != null && !apiKey.isBlank()) {
				builder.header("Authorization", "Bearer " + apiKey);
			}
			HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new EmbeddingException("OpenAI embeddings returned HTTP " + response.statusCode());
			}
			float[] vector = EmbeddingJson.parseOpenAiEmbedding(response.body());
			validateDimensions(vector);
			return vector;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmbeddingException("OpenAI embeddings request failed", e);
		} catch (IOException e) {
			throw new EmbeddingException("OpenAI embeddings request failed", e);
		}
	}

	private void validateDimensions(float[] vector) {
		if (vector.length != dimensions) {
			throw new EmbeddingException(
					"Expected embedding dimension " + dimensions + " but got " + vector.length);
		}
	}
}
