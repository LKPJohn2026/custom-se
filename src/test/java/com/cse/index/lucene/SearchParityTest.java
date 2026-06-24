package com.cse.index.lucene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.index.IndexDocument;
import com.cse.index.InvertedIndex;
import com.cse.index.InvertedIndex.SearchResult;
import com.cse.index.QueryMode;
import com.cse.index.SearchHit;
import com.cse.index.SearchOptions;
import com.cse.index.SearchQuery;
import com.cse.stem.FileStemmer;

/**
 * Validates that Lucene and InvertedIndex return the same result locations on a
 * shared corpus.
 */
public class SearchParityTest {
	@TempDir
	Path tempDir;

	private final InvertedIndex legacy = new InvertedIndex();
	private LuceneIndexStore lucene;

	@BeforeEach
	public void setUp() throws Exception {
		lucene = new LuceneIndexStore();
		lucene.open(tempDir);
		indexDoc("/docs/a.txt", "hello world hello");
		indexDoc("/docs/b.txt", "hello there");
		indexDoc("/docs/c.txt", "goodbye moon");
		lucene.commit();
	}

	@AfterEach
	public void tearDown() throws Exception {
		lucene.close();
	}

	@Test
	public void testExactParity() throws Exception {
		assertLocationsMatch("hello", QueryMode.EXACT);
	}

	@Test
	public void testPartialParity() throws Exception {
		assertLocationsMatch("hel", QueryMode.PARTIAL);
	}

	private void indexDoc(String location, String body) throws Exception {
		legacy.addAllWords(FileStemmer.listStems(body), location, 1);
		lucene.addDocument(new IndexDocument(location, location, "", body, System.currentTimeMillis()));
	}

	private void assertLocationsMatch(String query, QueryMode mode) throws Exception {
		Set<String> legacyLocs = legacy
				.exactIndex(FileStemmer.uniqueStems(query))
				.stream()
				.map(SearchResult::getLocation)
				.collect(Collectors.toSet());
		if (mode == QueryMode.PARTIAL) {
			legacyLocs = legacy.partialIndex(FileStemmer.uniqueStems(query)).stream()
					.map(SearchResult::getLocation)
					.collect(Collectors.toSet());
		}

		List<SearchHit> luceneHits = lucene.search(new SearchQuery(query, mode), SearchOptions.defaults());
		Set<String> luceneLocs = luceneHits.stream().map(SearchHit::location).collect(Collectors.toSet());

		assertEquals(legacyLocs, luceneLocs, "location mismatch for query=" + query + " mode=" + mode);
	}
}
