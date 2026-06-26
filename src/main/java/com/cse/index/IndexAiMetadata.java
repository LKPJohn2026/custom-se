package com.cse.index;

import java.time.Instant;

/**
 * Application metadata stored alongside the Lucene directory ({@code meta.json}).
 */
public record IndexAiMetadata(
		int indexVersion,
		String embeddingProvider,
		String embeddingModel,
		int vectorDimensions,
		int chunkTargetChars,
		int chunkOverlapChars,
		String indexedAt) {

	public static IndexAiMetadata chunkIndexDefaults() {
		return new IndexAiMetadata(3, "", "", 0, 2000, 200, Instant.now().toString());
	}

	public static IndexAiMetadata chunkIndex(ChunkingSettings chunking) {
		return new IndexAiMetadata(3, "", "", 0, chunking.targetChars(), chunking.overlapChars(),
				Instant.now().toString());
	}

	public record ChunkingSettings(int targetChars, int overlapChars) {
		public static ChunkingSettings defaults() {
			return new ChunkingSettings(2000, 200);
		}
	}
}
