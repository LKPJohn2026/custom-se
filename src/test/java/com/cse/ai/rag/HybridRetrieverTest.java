package com.cse.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.ai.chunk.Chunk;
import com.cse.index.lucene.LuceneIndexStore;

public class HybridRetrieverTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore store;
	private HybridRetriever retriever;

	@BeforeEach
	public void setUp() throws Exception {
		store = new LuceneIndexStore();
		store.open(tempDir);
		retriever = new HybridRetriever(store);
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testLexicalRetrieve() throws Exception {
		String loc = "/docs/guide.txt";
		store.addChunks(java.util.List.of(
				new Chunk(loc + "#0", loc, loc, "Guide", "search engine indexing", 0, 0, 1L)));
		store.commit();

		var hits = retriever.retrieve("search", 5);
		assertFalse(hits.isEmpty());
		assertEquals(loc, hits.get(0).chunk().location());
		assertEquals("search engine indexing", hits.get(0).chunk().text());
	}
}
