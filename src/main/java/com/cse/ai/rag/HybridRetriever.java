package com.cse.ai.rag;

import java.io.IOException;
import java.util.List;

import com.cse.index.IndexStore;
import com.cse.index.QueryMode;
import com.cse.index.SearchQuery;

/**
 * Retrieves ranked chunks for RAG. Phase 1: lexical (BM25) search only.
 */
public final class HybridRetriever {
	private final IndexStore index;

	public HybridRetriever(IndexStore index) {
		this.index = index;
	}

	public List<ScoredChunk> retrieve(String query, int topK) throws IOException {
		if (query == null || query.isBlank()) {
			return List.of();
		}
		return index.searchChunks(new SearchQuery(query.strip(), QueryMode.EXACT), topK);
	}

	public List<ScoredChunk> retrieve(SearchQuery query, int topK) throws IOException {
		return index.searchChunks(query, topK);
	}
}
