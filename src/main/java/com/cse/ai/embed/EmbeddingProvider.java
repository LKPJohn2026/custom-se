package com.cse.ai.embed;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates vector embeddings for chunk text and user queries.
 */
public interface EmbeddingProvider {

	String providerId();

	String model();

	int dimensions();

	float[] embed(String text);

	default List<float[]> embedBatch(List<String> texts) {
		List<float[]> vectors = new ArrayList<>(texts.size());
		for (String text : texts) {
			vectors.add(embed(text));
		}
		return vectors;
	}
}
