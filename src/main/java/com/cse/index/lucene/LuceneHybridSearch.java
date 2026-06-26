package com.cse.index.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.cse.ai.chunk.Chunk;
import com.cse.ai.rag.ScoredChunk;
import com.cse.index.QueryMode;
import com.cse.index.SearchQuery;
import com.cse.stem.FileStemmer;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Builds lexical and vector queries over chunk documents.
 */
public final class LuceneHybridSearch {
	private LuceneHybridSearch() {
	}

	public static List<ScoredChunk> searchLexical(IndexReader reader, Analyzer analyzer, SearchQuery query, int topK)
			throws IOException {
		if (query.raw() == null || query.raw().isBlank() || topK <= 0) {
			return List.of();
		}
		Query luceneQuery = buildFieldQuery(analyzer, query, LuceneSchema.FIELD_TEXT);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs topDocs = searcher.search(luceneQuery, topK);
		return toScoredChunks(reader, topDocs, true, false);
	}

	public static List<ScoredChunk> searchVector(IndexReader reader, float[] queryVector, int topK) throws IOException {
		if (queryVector == null || queryVector.length == 0 || topK <= 0) {
			return List.of();
		}
		Query vectorQuery = new KnnFloatVectorQuery(LuceneSchema.FIELD_VECTOR, queryVector, topK);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs topDocs = searcher.search(vectorQuery, topK);
		return toScoredChunks(reader, topDocs, false, true);
	}

	private static List<ScoredChunk> toScoredChunks(IndexReader reader, TopDocs topDocs, boolean lexical,
			boolean vector) throws IOException {
		List<ScoredChunk> hits = new ArrayList<>();
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			Document doc = reader.storedFields().document(scoreDoc.doc);
			Chunk chunk = toChunk(doc);
			double score = scoreDoc.score;
			double lexicalScore = lexical ? score : 0.0;
			double vectorScore = vector ? score : 0.0;
			hits.add(new ScoredChunk(chunk, score, lexicalScore, vectorScore));
		}
		return hits;
	}

	private static Chunk toChunk(Document doc) {
		return LuceneSchema.chunkFromDocument(doc);
	}

	private static Query buildFieldQuery(Analyzer analyzer, SearchQuery query, String field) throws IOException {
		String raw = query.raw().strip();
		if (query.mode() == QueryMode.PHRASE && raw.length() >= 2
				&& raw.startsWith("\"") && raw.endsWith("\"")) {
			String inner = raw.substring(1, raw.length() - 1);
			return buildPhraseQuery(analyzer, inner, field);
		}
		var stems = FileStemmer.uniqueStems(raw);
		if (stems.isEmpty()) {
			return new TermQuery(new Term(field, raw.toLowerCase()));
		}
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (String stem : stems) {
			Query termQuery = query.mode() == QueryMode.PARTIAL
					? new PrefixQuery(new Term(field, stem))
					: new TermQuery(new Term(field, stem));
			builder.add(termQuery, BooleanClause.Occur.MUST);
		}
		return builder.build();
	}

	private static PhraseQuery buildPhraseQuery(Analyzer analyzer, String text, String field) throws IOException {
		List<String> tokens = analyze(analyzer, text, field);
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		for (String token : tokens) {
			builder.add(new Term(field, token));
		}
		return builder.build();
	}

	private static List<String> analyze(Analyzer analyzer, String text, String field) throws IOException {
		List<String> tokens = new ArrayList<>();
		try (TokenStream stream = analyzer.tokenStream(field, text)) {
			CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				tokens.add(term.toString());
			}
			stream.end();
		}
		return tokens;
	}
}
