package com.cse.ai.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cse.ai.chunk.Chunk;

public class PromptBuilderTest {
	@Test
	public void testTrimsLowestScoreChunksFirst() {
		Chunk high = new Chunk("h", "p", "/high", "High", "HIGH CONTENT", 0, 0, 1L);
		Chunk low = new Chunk("l", "p", "/low", "Low", "LOW CONTENT LONG TEXT HERE", 1, 0, 1L);
		List<ScoredChunk> sources = List.of(
				new ScoredChunk(low, 0.1, 0.1, 0.0),
				new ScoredChunk(high, 0.9, 0.9, 0.0));
		String prompt = PromptBuilder.userPrompt(sources, "question?", 40);
		assertTrue(prompt.contains("/high"));
		assertFalse(prompt.contains("/low"));
	}
}
