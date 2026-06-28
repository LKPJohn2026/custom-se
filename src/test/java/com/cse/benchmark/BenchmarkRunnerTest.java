package com.cse.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.ai.chunk.Chunk;
import com.cse.index.IndexDocument;
import com.cse.index.lucene.LuceneIndexStore;

public class BenchmarkRunnerTest {
	@TempDir
	Path tempDir;

	private LuceneIndexStore store;

	@BeforeEach
	public void setUp() throws Exception {
		store = new LuceneIndexStore();
		store.open(tempDir);
		String loc = tempDir.resolve("doc.txt").toString();
		store.addDocument(new IndexDocument(loc, loc, "Doc", "alpha beta gamma delta", 1L));
		store.commit();
	}

	@AfterEach
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void testRunWithQueryFile(@TempDir Path queryDir) throws Exception {
		Path queryFile = queryDir.resolve("queries.txt");
		Files.writeString(queryFile, "alpha\nbeta\n# comment\n");

		BenchmarkConfig config = new BenchmarkConfig(queryFile, null, 2, 10, false, 10);
		BenchmarkReport report = new BenchmarkRunner().run(store, config);

		assertEquals(1, report.documents());
		assertTrue(report.latency().count() == 10);
		assertTrue(report.latency().medianMs() >= 0);
		assertTrue(report.oneLiner().contains("documents"));
		assertTrue(report.details().contains("median="));
	}

	@Test
	public void testFallbackQueriesFromIndexTerms() throws Exception {
		String loc = tempDir.resolve("two.txt").toString();
		Chunk chunk = new Chunk(loc + "#0", loc, loc, "Two", "epsilon zeta", 0, 0, 1L);
		store.addChunks(List.of(chunk));
		store.commit();

		BenchmarkConfig config = new BenchmarkConfig(null, null, 1, 5, false, 10);
		BenchmarkReport report = new BenchmarkRunner().run(store, config);

		assertTrue(report.queryCount() > 0);
		assertEquals(2, report.documents());
	}
}
