package com.cse.ai.rag;

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
import com.cse.ai.llm.MockLlmClient;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.profile.AiSettings;
import com.cse.index.lucene.LuceneIndexStore;

public class RagServiceTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore store;
	private RagService ragService;

	@BeforeEach
	public void setUp() throws Exception {
		store = new LuceneIndexStore();
		store.open(tempDir);
		AiSettings settings = AiSettings.load();
		ragService = new RagService(store, settings);
		var embedder = new FixedEmbeddingProvider(8);
		String loc = "/doc.txt";
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Guide", "search engines index text", 0, 0, 1L);
		store.addChunks(List.of(chunk), List.of(embedder.embed("search engines index text")), embedder);
		store.commit();
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testAsk() throws Exception {
		var embedder = new FixedEmbeddingProvider(8);
		AiProfile profile = new AiProfile("test", "Test", embedder,
				new MockLlmClient("Search engines index text."), false);
		RagResponse response = ragService.ask("search engines", profile, null);
		assertTrue(response.answer().contains("Search engines"));
		assertEquals(1, response.sources().size());
		assertEquals("test", response.stackId());
	}
}
