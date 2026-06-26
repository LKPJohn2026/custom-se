package com.cse.index.lucene;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import com.cse.ai.chunk.Chunk;

public class LuceneSchemaTest {
	@Test
	public void testChunkDocumentWithVector() {
		Chunk chunk = new Chunk("id#0", "id", "/loc", "t", "body", 0, 0, 1L);
		float[] vector = new float[] { 0.1f, 0.2f, 0.3f };
		Document doc = LuceneSchema.toLuceneChunkDocument(chunk, vector);
		assertNotNull(doc.getField(LuceneSchema.FIELD_VECTOR));
	}
}
