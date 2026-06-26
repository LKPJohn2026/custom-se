package com.cse.index.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.index.IndexAiMetadata;

public class IndexMetadataIOTest {
	@TempDir
	Path tempDir;

	@Test
	public void testRoundTrip() throws Exception {
		Path metaFile = tempDir.resolve("meta.json");
		IndexAiMetadata meta = new IndexAiMetadata(3, "ollama", "nomic-embed-text", 768, 2000, 200,
				"2026-01-01T00:00:00Z");
		IndexMetadataIO.write(metaFile, meta);
		var loaded = IndexMetadataIO.read(metaFile);
		assertTrue(loaded.isPresent());
		assertEquals(3, loaded.get().indexVersion());
		assertEquals("ollama", loaded.get().embeddingProvider());
		assertEquals(768, loaded.get().vectorDimensions());
		assertTrue(Files.isRegularFile(metaFile));
	}
}
