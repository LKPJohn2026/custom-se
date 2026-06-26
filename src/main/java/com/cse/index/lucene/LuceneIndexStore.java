package com.cse.index.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.cse.ai.chunk.Chunk;
import com.cse.ai.rag.ScoredChunk;
import com.cse.index.IndexAiMetadata;
import com.cse.index.IndexDocument;
import com.cse.index.IndexStore;
import com.cse.index.QueryMode;
import com.cse.index.SearchHit;
import com.cse.index.SearchOptions;
import com.cse.index.SearchQuery;
import com.cse.server.view.YamlWriter;
import com.cse.stem.FileStemmer;

/**
 * Lucene-backed {@link IndexStore} implementation.
 */
public class LuceneIndexStore implements IndexStore {
	private final Analyzer analyzer;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private Path indexDir;
	private Directory directory;
	private IndexWriter writer;
	private DirectoryReader reader;
	private IndexAiMetadata indexMetadata;

	public LuceneIndexStore() {
		this(LuceneSchema.analyzer());
	}

	public LuceneIndexStore(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override
	public void open(Path indexDir) throws IOException {
		lock.writeLock().lock();
		try {
			if (isOpen()) {
				throw new IllegalStateException("Index already open");
			}
			this.indexDir = indexDir;
			Files.createDirectories(indexDir);
			this.directory = FSDirectory.open(indexDir);
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			this.writer = new IndexWriter(directory, config);
			Path metaFile = indexDir.resolve("meta.json");
			this.indexMetadata = IndexMetadataIO.read(metaFile).orElse(null);
			refreshReader();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void commit() throws IOException {
		lock.writeLock().lock();
		try {
			ensureOpen();
			writer.commit();
			refreshReader();
			if (indexMetadata != null) {
				IndexMetadataIO.write(indexDir.resolve("meta.json"), indexMetadata);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void close() throws IOException {
		lock.writeLock().lock();
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (writer != null) {
				writer.close();
				writer = null;
			}
			if (directory != null) {
				directory.close();
				directory = null;
			}
			indexDir = null;
			indexMetadata = null;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean isOpen() {
		return writer != null;
	}

	@Override
	public Path indexDirectory() {
		return indexDir;
	}

	@Override
	public void addDocument(IndexDocument doc) throws IOException {
		lock.writeLock().lock();
		try {
			ensureOpen();
			writer.updateDocument(new Term(LuceneSchema.FIELD_ID, doc.id()),
					LuceneSchema.toLuceneDocument(doc));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public IndexAiMetadata indexMetadata() {
		return indexMetadata;
	}

	@Override
	public void addChunks(List<Chunk> chunks) throws IOException {
		lock.writeLock().lock();
		try {
			ensureOpen();
			for (Chunk chunk : chunks) {
				writer.updateDocument(new Term(LuceneSchema.FIELD_ID, chunk.chunkId()),
						LuceneSchema.toLuceneChunkDocument(chunk));
			}
			if (!chunks.isEmpty()) {
				indexMetadata = IndexAiMetadata.chunkIndexDefaults();
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void deleteDocument(String id) throws IOException {
		lock.writeLock().lock();
		try {
			ensureOpen();
			writer.deleteDocuments(new Term(LuceneSchema.FIELD_ID, id));
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public long documentCount() {
		lock.readLock().lock();
		try {
			return reader == null ? 0 : reader.numDocs();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<String> listTerms() {
		lock.readLock().lock();
		try {
			ensureReader();
			Set<String> terms = new TreeSet<>();
			var leaves = reader.leaves();
			for (var leafCtx : leaves) {
				Terms fieldTerms = leafCtx.reader().terms(LuceneSchema.FIELD_BODY);
				if (fieldTerms == null) {
					continue;
				}
				TermsEnum termsEnum = fieldTerms.iterator();
				while (termsEnum.next() != null) {
					terms.add(termsEnum.term().utf8ToString());
				}
			}
			return terms;
		} catch (IOException e) {
			return Set.of();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<String> listLocations() {
		lock.readLock().lock();
		try {
			ensureReader();
			Set<String> locations = new TreeSet<>();
			for (int i = 0; i < reader.maxDoc(); i++) {
				Document doc = reader.storedFields().document(i);
				if (doc != null) {
					String loc = doc.get(LuceneSchema.FIELD_LOCATION);
					if (loc != null) {
						locations.add(loc);
					}
				}
			}
			return locations;
		} catch (IOException e) {
			return Set.of();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<String> locationsForTerm(String term) {
		try {
			SearchQuery query = new SearchQuery(term, QueryMode.EXACT);
			List<SearchHit> hits = search(query, new SearchOptions(SearchOptions.MAX_LIMIT, 0, false, false));
			Set<String> locations = new TreeSet<>();
			for (SearchHit hit : hits) {
				locations.add(hit.location());
			}
			return locations;
		} catch (IOException e) {
			return Set.of();
		}
	}

	@Override
	public List<SearchHit> search(SearchQuery query, SearchOptions options) throws IOException {
		lock.readLock().lock();
		try {
			ensureReader();
			if (query.raw() == null || query.raw().isBlank()) {
				return List.of();
			}
			Query luceneQuery = buildQuery(query);
			IndexSearcher searcher = new IndexSearcher(reader);
			int fetch = options.offset() + options.limit();
			TopDocs topDocs = searcher.search(luceneQuery, fetch);
			List<SearchHit> hits = new ArrayList<>();
			ScoreDoc[] scores = topDocs.scoreDocs;
			for (int i = options.offset(); i < scores.length; i++) {
				Document doc = reader.storedFields().document(scores[i].doc);
				String location = doc.get(LuceneSchema.FIELD_LOCATION);
				hits.add(new SearchHit(location, scores[i].score, 0));
			}
			Collections.sort(hits);
			if (options.reverse()) {
				Collections.reverse(hits);
			}
			return hits;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<ScoredChunk> searchChunks(SearchQuery query, int topK) throws IOException {
		lock.readLock().lock();
		try {
			ensureReader();
			if (query.raw() == null || query.raw().isBlank() || topK <= 0) {
				return List.of();
			}
			Query luceneQuery = buildFieldQuery(query, LuceneSchema.FIELD_TEXT);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs topDocs = searcher.search(luceneQuery, topK);
			List<ScoredChunk> hits = new ArrayList<>();
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = reader.storedFields().document(scoreDoc.doc);
				Chunk chunk = toChunk(doc);
				double score = scoreDoc.score;
				hits.add(new ScoredChunk(chunk, score, score, 0.0));
			}
			return hits;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void exportJson(Path path) throws IOException {
		StringBuilder json = new StringBuilder("{\n");
		boolean firstWord = true;
		for (String word : listTerms()) {
			if (!firstWord) {
				json.append(",\n");
			}
			firstWord = false;
			json.append("  \"").append(escapeJson(word)).append("\": {");
			boolean firstLoc = true;
			for (String loc : locationsForTerm(word)) {
				if (!firstLoc) {
					json.append(',');
				}
				firstLoc = false;
				json.append("\n    \"").append(escapeJson(loc)).append("\": {}");
			}
			json.append("\n  }");
		}
		json.append("\n}\n");
		Files.writeString(path, json.toString());
	}

	@Override
	public void exportYaml(Path path) throws IOException {
		Map<String, TreeMap<String, TreeSet<Integer>>> snapshot = new TreeMap<>();
		for (String word : listTerms()) {
			TreeMap<String, TreeSet<Integer>> locs = new TreeMap<>();
			for (String loc : locationsForTerm(word)) {
				locs.put(loc, new TreeSet<>());
			}
			snapshot.put(word, locs);
		}
		YamlWriter.writeIndex(snapshot, Files.newBufferedWriter(path));
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private Query buildQuery(SearchQuery query) throws IOException {
		return buildFieldQuery(query, LuceneSchema.FIELD_BODY);
	}

	private Query buildFieldQuery(SearchQuery query, String field) throws IOException {
		String raw = query.raw().strip();
		if (query.mode() == QueryMode.PHRASE && raw.length() >= 2
				&& raw.startsWith("\"") && raw.endsWith("\"")) {
			String inner = raw.substring(1, raw.length() - 1);
			return buildPhraseQuery(inner, field);
		}
		Set<String> stems = FileStemmer.uniqueStems(raw);
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

	private PhraseQuery buildPhraseQuery(String text, String field) throws IOException {
		List<String> tokens = analyze(text, field);
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		for (String token : tokens) {
			builder.add(new Term(field, token));
		}
		return builder.build();
	}

	private List<String> analyze(String text, String field) throws IOException {
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

	private static Chunk toChunk(Document doc) {
		String chunkId = doc.get(LuceneSchema.FIELD_CHUNK_ID);
		String parentId = doc.get(LuceneSchema.FIELD_PARENT_ID);
		String location = doc.get(LuceneSchema.FIELD_LOCATION);
		String title = doc.get(LuceneSchema.FIELD_TITLE);
		String text = doc.get(LuceneSchema.FIELD_TEXT);
		String sequenceRaw = doc.get(LuceneSchema.FIELD_SEQUENCE);
		String indexedAtRaw = doc.get(LuceneSchema.FIELD_INDEXED_AT);
		int sequence = sequenceRaw == null ? 0 : Integer.parseInt(sequenceRaw);
		long indexedAt = indexedAtRaw == null ? 0L : Long.parseLong(indexedAtRaw);
		return new Chunk(chunkId, parentId, location, title == null ? "" : title,
				text == null ? "" : text, sequence, 0, indexedAt);
	}

	private void refreshReader() throws IOException {
		if (reader == null) {
			reader = DirectoryReader.open(writer);
		} else {
			DirectoryReader newReader = DirectoryReader.openIfChanged(reader, writer);
			if (newReader != null) {
				reader.close();
				reader = newReader;
			}
		}
	}

	private void ensureOpen() {
		if (!isOpen()) {
			throw new IllegalStateException("Index is not open");
		}
	}

	private void ensureReader() throws IOException {
		ensureOpen();
		if (reader == null) {
			refreshReader();
		}
	}
}
