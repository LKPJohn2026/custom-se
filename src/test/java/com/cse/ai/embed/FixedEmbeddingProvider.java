package com.cse.ai.embed;

import java.util.List;

/**
 * Deterministic embeddings for unit tests (no HTTP).
 */
public final class FixedEmbeddingProvider implements EmbeddingProvider {
	private final String providerId;
	private final String model;
	private final int dimensions;

	public FixedEmbeddingProvider(int dimensions) {
		this("test", "fixed", dimensions);
	}

	public FixedEmbeddingProvider(String providerId, String model, int dimensions) {
		this.providerId = providerId;
		this.model = model;
		this.dimensions = dimensions;
	}

	@Override
	public String providerId() {
		return providerId;
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
		float[] vector = new float[dimensions];
		int seed = text == null ? 0 : text.hashCode();
		for (int i = 0; i < dimensions; i++) {
			vector[i] = ((seed + i * 31) % 1000) / 1000.0f;
		}
		return vector;
	}

	@Override
	public List<float[]> embedBatch(List<String> texts) {
		return texts.stream().map(this::embed).toList();
	}
}
