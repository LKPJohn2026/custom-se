package com.cse.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

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
}
