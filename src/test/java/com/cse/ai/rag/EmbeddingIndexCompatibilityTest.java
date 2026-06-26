package com.cse.ai.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cse.ai.embed.FixedEmbeddingProvider;
import com.cse.index.IndexAiMetadata;

public class EmbeddingIndexCompatibilityTest {
	@Test
	public void testMatchingMetadataEnablesVectors() {
		var meta = new IndexAiMetadata(3, "test", "fixed", 8, 2000, 200, "now");
		var provider = new FixedEmbeddingProvider(8);
		assertTrue(EmbeddingIndexCompatibility.vectorsEnabled(meta, provider));
	}

	@Test
	public void testDimensionMismatchDisablesVectors() {
		var meta = new IndexAiMetadata(3, "test", "fixed", 8, 2000, 200, "now");
		var provider = new FixedEmbeddingProvider(4);
		assertFalse(EmbeddingIndexCompatibility.vectorsEnabled(meta, provider));
	}
}
