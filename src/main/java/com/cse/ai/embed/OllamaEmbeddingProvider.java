package com.cse.ai.embed;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Embeddings via Ollama {@code POST /api/embeddings}.
 */
public final class OllamaEmbeddingProvider implements EmbeddingProvider {
	private final HttpClient http;
	private final URI embedUri;
	private final String model;
	private final int dimensions;

	public OllamaEmbeddingProvider(String baseUrl, String model, int dimensions) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
				baseUrl, model, dimensions);
	}

	OllamaEmbeddingProvider(HttpClient http, String baseUrl, String model, int dimensions) {
		this.http = http;
		String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.embedUri = URI.create(normalized + "/api/embeddings");
		this.model = model;
		this.dimensions = dimensions;
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
	public int dimensions() {
		return dimensions;
	}

	@Override
	public float[] embed(String text) {
		try {
			HttpRequest request = HttpRequest.newBuilder(embedUri)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(EmbeddingJson.ollamaRequest(model, text)))
					.timeout(Duration.ofSeconds(60))
					.build();
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new EmbeddingException("Ollama embeddings returned HTTP " + response.statusCode());
			}
			float[] vector = EmbeddingJson.parseOllamaEmbedding(response.body());
			validateDimensions(vector);
			return vector;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmbeddingException("Ollama embeddings request failed", e);
		} catch (IOException e) {
			throw new EmbeddingException("Ollama embeddings request failed", e);
		}
	}

	private void validateDimensions(float[] vector) {
		if (vector.length != dimensions) {
			throw new EmbeddingException(
					"Expected embedding dimension " + dimensions + " but got " + vector.length);
		}
	}
}
