package com.cse.ai.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cse.index.IndexDocument;

public class ChunkerTest {
	private final Chunker chunker = new DefaultChunker();

	@Test
	public void testSingleShortDocument() {
		IndexDocument doc = new IndexDocument("/a.txt", "/a.txt", "Title", "hello world", 1L);
		var chunks = chunker.chunk(doc, ChunkingOptions.defaults());
		assertEquals(1, chunks.size());
		assertEquals("hello world", chunks.get(0).text());
		assertEquals("/a.txt#0", chunks.get(0).chunkId());
	}

	@Test
	public void testSplitsLongBody() {
		String body = "word ".repeat(600);
		IndexDocument doc = new IndexDocument("/b.txt", "/b.txt", "", body, 1L);
		var options = new ChunkingOptions(500, 50, 50);
		var chunks = chunker.chunk(doc, options);
		assertTrue(chunks.size() > 1);
		for (int i = 0; i < chunks.size(); i++) {
			assertEquals(i, chunks.get(i).sequence());
			assertEquals("/b.txt#" + i, chunks.get(i).chunkId());
		}
	}

	@Test
	public void testEmptyBody() {
		IndexDocument doc = new IndexDocument("/c.txt", "/c.txt", "", "   ", 1L);
		var chunks = chunker.chunk(doc, ChunkingOptions.defaults());
		assertEquals(1, chunks.size());
		assertEquals("", chunks.get(0).text());
	}

	@Test
	public void testMaxChunksCap() {
		String body = "x ".repeat(10_000);
		IndexDocument doc = new IndexDocument("/d.txt", "/d.txt", "", body, 1L);
		var chunks = chunker.chunk(doc, new ChunkingOptions(100, 10, 3));
		assertEquals(3, chunks.size());
	}
}
