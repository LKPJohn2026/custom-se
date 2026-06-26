package com.cse.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.cse.ai.chunk.Chunk;

/**
 * Application-facing index API. Runtime implementation: {@code LuceneIndexStore}.
 */
public interface IndexStore extends AutoCloseable {

	void open(Path indexDir) throws IOException;

	void commit() throws IOException;

	@Override
	void close() throws IOException;

	boolean isOpen();

	Path indexDirectory();

	void addDocument(IndexDocument doc) throws IOException;

	void addChunks(List<Chunk> chunks) throws IOException;

	void deleteDocument(String id) throws IOException;

	long documentCount();

	Set<String> listTerms();

	Set<String> listLocations();

	Set<String> locationsForTerm(String term);

	List<SearchHit> search(SearchQuery query, SearchOptions options) throws IOException;

	void exportJson(Path path) throws IOException;

	void exportYaml(Path path) throws IOException;
}
