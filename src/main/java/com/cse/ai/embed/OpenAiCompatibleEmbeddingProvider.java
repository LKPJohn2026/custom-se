package com.cse.ai.embed;

/**
 * OpenAI-compatible embeddings for local servers such as LM Studio.
 */
public final class OpenAiCompatibleEmbeddingProvider extends OpenAiEmbeddingProvider {

	public OpenAiCompatibleEmbeddingProvider(String baseUrl, String model, int dimensions) {
		super(java.net.http.HttpClient.newBuilder().build(),
				embeddingsUrl(baseUrl), null, model, dimensions);
	}

	@Override
	public String providerId() {
		return "openai-compatible";
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
