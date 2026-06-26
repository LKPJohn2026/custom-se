package com.cse.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.VectorSimilarityFunction;

import com.cse.ai.chunk.Chunk;
import com.cse.index.IndexDocument;

/**
 * Lucene field names and document mapping.
 */
public final class LuceneSchema {
	public static final String FIELD_ID = "id";
	public static final String FIELD_LOCATION = "location";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY = "body";
	public static final String FIELD_INDEXED_AT = "indexedAt";
	public static final String FIELD_CHUNK_ID = "chunkId";
	public static final String FIELD_PARENT_ID = "parentId";
	public static final String FIELD_TEXT = "text";
	public static final String FIELD_SEQUENCE = "sequence";
	public static final String FIELD_VECTOR = "vector";
	public static final int DEFAULT_VECTOR_DIMENSIONS = 768;

	private LuceneSchema() {
	}

	public static Analyzer analyzer() {
		return new EnglishAnalyzer();
	}

	public static Document toLuceneDocument(IndexDocument doc) {
		Document document = new Document();
		document.add(new StringField(FIELD_ID, doc.id(), Field.Store.YES));
		document.add(new StringField(FIELD_LOCATION, doc.location(), Field.Store.YES));
		String title = doc.title() == null ? "" : doc.title();
		document.add(new TextField(FIELD_TITLE, title, Field.Store.YES));
		document.add(new TextField(FIELD_BODY, doc.body(), Field.Store.YES));
		document.add(new LongPoint(FIELD_INDEXED_AT, doc.indexedAt()));
		document.add(new StoredField(FIELD_INDEXED_AT, doc.indexedAt()));
		return document;
	}

	public static Document toLuceneChunkDocument(Chunk chunk) {
		return toLuceneChunkDocument(chunk, null);
	}

	public static Document toLuceneChunkDocument(Chunk chunk, float[] vector) {
		Document document = new Document();
		document.add(new StringField(FIELD_CHUNK_ID, chunk.chunkId(), Field.Store.YES));
		document.add(new StringField(FIELD_ID, chunk.chunkId(), Field.Store.YES));
		document.add(new StringField(FIELD_PARENT_ID, chunk.parentId(), Field.Store.YES));
		document.add(new StringField(FIELD_LOCATION, chunk.location(), Field.Store.YES));
		String title = chunk.title() == null ? "" : chunk.title();
		document.add(new TextField(FIELD_TITLE, title, Field.Store.YES));
		document.add(new TextField(FIELD_TEXT, chunk.text(), Field.Store.YES));
		document.add(new IntPoint(FIELD_SEQUENCE, chunk.sequence()));
		document.add(new StoredField(FIELD_SEQUENCE, chunk.sequence()));
		document.add(new LongPoint(FIELD_INDEXED_AT, chunk.indexedAt()));
		document.add(new StoredField(FIELD_INDEXED_AT, chunk.indexedAt()));
		if (vector != null) {
			document.add(new KnnFloatVectorField(FIELD_VECTOR, vector, VectorSimilarityFunction.COSINE));
		}
		return document;
	}
}
