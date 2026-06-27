package com.cse.ai.embed;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Embeddings via Voyage AI {@code POST /v1/embeddings} (Anthropic's recommended partner).
 */
public final class VoyageEmbeddingProvider implements EmbeddingProvider {
	private final HttpClient http;
	private final URI embedUri;
	private final String apiKey;
	private final String model;
	private final int dimensions;

	public VoyageEmbeddingProvider(String apiKey, String model, int dimensions) {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
				URI.create("https://api.voyageai.com/v1/embeddings"), apiKey, model, dimensions);
	}

	VoyageEmbeddingProvider(HttpClient http, String apiKey, String model, int dimensions) {
		this(http, URI.create("https://api.voyageai.com/v1/embeddings"), apiKey, model, dimensions);
	}

	VoyageEmbeddingProvider(HttpClient http, URI embedUri, String apiKey, String model, int dimensions) {
		this.http = http;
		this.embedUri = embedUri;
		this.apiKey = apiKey;
		this.model = model;
		this.dimensions = dimensions;
	}

	@Override
	public String providerId() {
		return "voyage";
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
		return embedQuery(text);
	}

	@Override
	public float[] embedQuery(String text) {
		return request(List.of(text), "query").get(0);
	}

	@Override
	public List<float[]> embedBatchDocuments(List<String> texts) {
		return request(texts, "document");
	}

	private List<float[]> request(List<String> texts, String inputType) {
		try {
			HttpRequest request = HttpRequest.newBuilder(embedUri)
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(
							EmbeddingJson.voyageRequest(model, texts, inputType, dimensions)))
					.timeout(Duration.ofSeconds(60))
					.build();
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new EmbeddingException("Voyage embeddings returned HTTP " + response.statusCode());
			}
			List<float[]> vectors = EmbeddingJson.parseOpenAiEmbeddings(response.body());
			for (float[] vector : vectors) {
				validateDimensions(vector);
			}
			return vectors;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmbeddingException("Voyage embeddings request failed", e);
		} catch (IOException e) {
			throw new EmbeddingException("Voyage embeddings request failed", e);
		}
	}

	private void validateDimensions(float[] vector) {
		if (vector.length != dimensions) {
			throw new EmbeddingException(
					"Expected embedding dimension " + dimensions + " but got " + vector.length);
		}
	}
}
