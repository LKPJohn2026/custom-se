package com.cse.index.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.ai.chunk.Chunk;
import com.cse.ai.embed.FixedEmbeddingProvider;
import com.cse.index.IndexDocument;
import com.cse.index.QueryMode;
import com.cse.index.SearchOptions;
import com.cse.index.SearchQuery;

public class LuceneIndexStoreTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore store;

	@BeforeEach
	public void setUp() throws Exception {
		store = new LuceneIndexStore();
		store.open(tempDir);
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testAddAndSearch() throws Exception {
		String loc = "/data/hello.txt";
		store.addDocument(new IndexDocument(loc, loc, "Hello", "hello world hello", 1L));
		store.commit();

		var hits = store.search(new SearchQuery("hello", QueryMode.EXACT), SearchOptions.defaults());
		assertEquals(1, hits.size());
		assertEquals(loc, hits.get(0).location());
	}

	@Test
	public void testReopenAfterCommit() throws Exception {
		String loc = "/data/doc.txt";
		store.addDocument(new IndexDocument(loc, loc, "", "apple banana cherry", 1L));
		store.commit();
		store.close();

		store.open(tempDir);
		assertEquals(1, store.documentCount());
		assertTrue(store.listTerms().contains("appl"));
	}

	@Test
	public void testAddChunksWritesMetadataOnCommit() throws Exception {
		String loc = "/data/chunked.txt";
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Doc", "chunked content", 0, 0, 1L);
		store.addChunks(List.of(chunk));
		store.commit();
		store.close();

		store.open(tempDir);
		assertEquals(3, store.indexMetadata().indexVersion());
	}

	@Test
	public void testAddChunksWithEmbeddings() throws Exception {
		var embedder = new FixedEmbeddingProvider(8);
		String loc = "/data/vector.txt";
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Doc", "vector content", 0, 0, 1L);
		store.addChunks(List.of(chunk), List.of(embedder.embed("vector content")), embedder);
		store.commit();
		assertEquals("test", store.indexMetadata().embeddingProvider());
		assertEquals(8, store.indexMetadata().vectorDimensions());
	}
}
