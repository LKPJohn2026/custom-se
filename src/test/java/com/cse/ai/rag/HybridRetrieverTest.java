package com.cse.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.ai.chunk.Chunk;
import com.cse.ai.embed.FixedEmbeddingProvider;
import com.cse.index.lucene.LuceneIndexStore;

public class HybridRetrieverTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore store;
	private FixedEmbeddingProvider embedder;

	@BeforeEach
	public void setUp() throws Exception {
		store = new LuceneIndexStore();
		store.open(tempDir);
		embedder = new FixedEmbeddingProvider(8);
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testLexicalRetrieve() throws Exception {
		indexChunk("search engine indexing", "search");
		var retriever = new HybridRetriever(store);
		var hits = retriever.retrieve("search", 5);
		assertFalse(hits.isEmpty());
		assertEquals("/docs/guide.txt", hits.get(0).chunk().location());
	}

	@Test
	public void testHybridRetrieveWithVectors() throws Exception {
		indexChunk("neural retrieval semantic matching", "semantic");
		var retriever = new HybridRetriever(store, embedder);
		var hits = retriever.retrieve("semantic", 5);
		assertFalse(hits.isEmpty());
		assertTrue(hits.get(0).score() > 0);
	}

	@Test
	public void testMismatchFallsBackToLexicalOnly() throws Exception {
		indexChunk("keyword fallback path", "fallback");
		var wrongDims = new FixedEmbeddingProvider(4);
		var retriever = new HybridRetriever(store, wrongDims);
		var hits = retriever.retrieve("fallback", 5);
		assertFalse(hits.isEmpty());
		assertEquals(0.0, hits.get(0).vectorScore());
	}

	private void indexChunk(String text, String keyword) throws Exception {
		String loc = "/docs/guide.txt";
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Guide", text, 0, 0, 1L);
		store.addChunks(List.of(chunk), List.of(embedder.embed(text)), embedder);
		store.commit();
	}
}
