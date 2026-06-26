package com.cse.ai.rag;

import java.io.IOException;
import java.util.List;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.index.IndexStore;
import com.cse.index.QueryMode;
import com.cse.index.SearchQuery;

/**
 * Retrieves ranked chunks for RAG using lexical search and optional vector RRF merge.
 */
public final class HybridRetriever {
	private final IndexStore index;
	private final EmbeddingProvider embedder;

	public HybridRetriever(IndexStore index) {
		this(index, null);
	}

	public HybridRetriever(IndexStore index, EmbeddingProvider embedder) {
		this.index = index;
		this.embedder = embedder;
	}

	public List<ScoredChunk> retrieve(String query, int topK) throws IOException {
		if (query == null || query.isBlank()) {
			return List.of();
		}
		return retrieve(new SearchQuery(query.strip(), QueryMode.EXACT), topK);
	}

	public List<ScoredChunk> retrieve(SearchQuery query, int topK) throws IOException {
		int fetchK = Math.max(topK, topK);
		List<ScoredChunk> lexical = index.searchChunks(query, fetchK);
		if (embedder == null || !EmbeddingIndexCompatibility.vectorsEnabled(index.indexMetadata(), embedder)) {
			return lexical.size() <= topK ? lexical : lexical.subList(0, topK);
		}
		float[] queryVector = embedder.embed(query.raw());
		List<ScoredChunk> vector = index.searchChunksByVector(queryVector, fetchK);
		return RrfMerger.merge(lexical, vector, topK);
	}
}
