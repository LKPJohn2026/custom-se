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
import com.cse.ai.llm.MockLlmClient;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.profile.AiSettings;

public class RagServiceContractTest {
	@TempDir
	Path tempDir;

	private com.cse.index.lucene.LuceneIndexStore store;
	private RagService ragService;

	@BeforeEach
	public void setUp() throws Exception {
		store = new com.cse.index.lucene.LuceneIndexStore();
		store.open(tempDir);
		AiSettings settings = AiSettings.load();
		ragService = new RagService(store, settings);
		indexChunk("/a.txt", "alpha beta gamma", 0.9);
		indexChunk("/b.txt", "delta epsilon zeta", 0.1);
		store.commit();
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testAskReturnsSourcesAndStack() throws Exception {
		AiProfile profile = profile("Answer from alpha.");
		RagResponse response = ragService.ask("alpha", profile, null);
		assertTrue(response.answer().contains("alpha"));
		assertFalse(response.sources().isEmpty());
		assertEquals("test", response.stackId());
		assertTrue(response.retrievalMs() >= 0);
		assertTrue(response.generationMs() >= 0);
	}

	@Test
	public void testPrepareStreamIncludesSources() throws Exception {
		AiProfile profile = profile("stream");
		RagStreamContext ctx = ragService.prepareStream("alpha", profile);
		assertFalse(ctx.sources().isEmpty());
		assertEquals("test", ctx.stackId());
		List<String> tokens = ctx.tokens().toList();
		assertFalse(tokens.isEmpty());
	}

	private AiProfile profile(String answer) {
		var embedder = new FixedEmbeddingProvider(8);
		return new AiProfile("test", "Test", embedder, new MockLlmClient(answer), false);
	}

	private void indexChunk(String loc, String text, double unused) throws Exception {
		var embedder = new FixedEmbeddingProvider(8);
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Title", text, 0, 0, 1L);
		store.addChunks(List.of(chunk), List.of(embedder.embed(text)), embedder);
	}
}
