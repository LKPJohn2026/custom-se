package com.cse.ai.rag;

import com.cse.ai.chunk.Chunk;

/**
 * One chunk retrieved for RAG with hybrid scores.
 */
public record ScoredChunk(
		Chunk chunk,
		double score,
		double lexicalScore,
		double vectorScore) {
}
