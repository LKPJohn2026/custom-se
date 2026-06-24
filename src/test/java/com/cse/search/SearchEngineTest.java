package com.cse.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.index.IndexDocument;
import com.cse.index.lucene.LuceneIndexStore;
import com.cse.server.meta.MetadataStore;
import com.cse.server.session.UserSessionData;

public class SearchEngineTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore index;
	private SearchEngine engine;

	@BeforeEach
	public void setUp() throws Exception {
		index = new LuceneIndexStore();
		index.open(tempDir);
		index.addDocument(new IndexDocument("/a", "/a", "", "hello world", 1L));
		index.commit();
		engine = new SearchEngine(index, new MetadataStore(), null);
	}

	@AfterEach
	public void tearDown() throws Exception {
		index.close();
	}

	@Test
	public void testSearchRecordsMetadata() throws Exception {
		UserSessionData session = new UserSessionData();
		var response = engine.search(SearchEngine.parseQuery("hello", true), 
				com.cse.index.SearchOptions.defaults(), session);
		assertEquals(1, response.results().size());
		assertEquals(1, session.searchHistory().size());
	}
}
