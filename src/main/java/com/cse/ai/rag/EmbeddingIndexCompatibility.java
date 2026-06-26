package com.cse.ai.rag;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.index.IndexAiMetadata;

/**
 * Determines whether vector search can run against the current index metadata.
 */
public final class EmbeddingIndexCompatibility {
	private EmbeddingIndexCompatibility() {
	}

	public static boolean vectorsEnabled(IndexAiMetadata metadata, EmbeddingProvider provider) {
		if (metadata == null || provider == null) {
			return false;
		}
		if (metadata.vectorDimensions() <= 0) {
			return false;
		}
		if (metadata.vectorDimensions() != provider.dimensions()) {
			return false;
		}
		if (!metadata.embeddingProvider().isBlank()
				&& !metadata.embeddingProvider().equals(provider.providerId())) {
			return false;
		}
		if (!metadata.embeddingModel().isBlank() && !metadata.embeddingModel().equals(provider.model())) {
			return false;
		}
		return true;
	}
}
