package com.cse.ai.rag;

import java.util.List;
import java.util.stream.Stream;

/**
 * Prepared RAG stream: retrieved sources plus token stream from the LLM.
 */
public record RagStreamContext(
		List<ScoredChunk> sources,
		Stream<String> tokens,
		String stackId,
		long retrievalMs) {
}
