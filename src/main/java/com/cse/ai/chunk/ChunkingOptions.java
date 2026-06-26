package com.cse.ai.chunk;

/**
 * Controls how {@link IndexDocument} bodies are split into {@link Chunk} records.
 */
public record ChunkingOptions(int targetChars, int overlapChars, int maxChunksPerDoc) {

	public static ChunkingOptions defaults() {
		return new ChunkingOptions(2000, 200, 50);
	}
}
