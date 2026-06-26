package com.cse.ai.embed;

import java.net.http.HttpClient;

/**
 * OpenAI-compatible embeddings for local servers such as LM Studio.
 */
public final class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
	private final OpenAiEmbeddingProvider delegate;

	public OpenAiCompatibleEmbeddingProvider(String baseUrl, String model, int dimensions) {
		this.delegate = new OpenAiEmbeddingProvider(HttpClient.newHttpClient(), embeddingsUrl(baseUrl), null, model,
				dimensions);
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
	public int dimensions() {
		return delegate.dimensions();
	}

	@Override
	public float[] embed(String text) {
		return delegate.embed(text);
	}

	private static String embeddingsUrl(String baseUrl) {
		String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		if (trimmed.endsWith("/embeddings")) {
			return trimmed;
		}
		if (trimmed.endsWith("/v1")) {
			return trimmed + "/embeddings";
		}
		return trimmed + "/v1/embeddings";
	}
}
