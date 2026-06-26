package com.cse.ai.chunk;

/**
 * One indexed passage derived from a parent {@link com.cse.index.IndexDocument}.
 */
public record Chunk(
		String chunkId,
		String parentId,
		String location,
		String title,
		String text,
		int sequence,
		int charOffset,
		long indexedAt) {
}
