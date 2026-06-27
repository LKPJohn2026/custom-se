package com.cse.ai.chunk;

import java.io.IOException;
import java.util.List;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.index.IndexDocument;
import com.cse.index.IndexStore;

/**
 * Chunks documents and optionally embeds them before writing to {@link IndexStore}.
 */
public final class ChunkIndexing {
	private static final Chunker CHUNKER = new DefaultChunker();

	private ChunkIndexing() {
	}

	public static void indexDocument(IndexStore store, IndexDocument doc) throws IOException {
		indexDocument(store, null, doc);
	}

	public static void indexDocument(IndexStore store, EmbeddingProvider embedder, IndexDocument doc)
			throws IOException {
		List<Chunk> chunks = CHUNKER.chunk(doc, ChunkingOptions.defaults());
		if (embedder == null) {
			store.addChunks(chunks);
			return;
		}
		List<String> texts = chunks.stream().map(Chunk::text).toList();
		store.addChunks(chunks, embedder.embedBatchDocuments(texts), embedder);
	}
}
