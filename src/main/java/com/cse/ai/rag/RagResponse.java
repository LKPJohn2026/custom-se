package com.cse.ai.rag;

import java.util.List;

/**
 * RAG answer with retrieval metadata.
 */
public record RagResponse(
		String answer,
		List<ScoredChunk> sources,
		long retrievalMs,
		long generationMs,
		String stackId) {
}
