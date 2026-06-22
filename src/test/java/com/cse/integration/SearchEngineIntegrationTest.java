package com.cse.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.cse.concurrent.WorkQueue;
import com.cse.index.InvertedIndex;
import com.cse.index.InvertedIndex.SearchResult;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.search.ThreadedSearchProcessor;
import com.cse.stem.FileStemmer;

/**
 * Integration tests that exercise multiple components together: the
 * multi-threaded indexer, the thread-safe index, the threaded search processor,
 * and JSON output.
 */
public class SearchEngineIntegrationTest {

	/** Writes a text file with the given content and returns its path. */
	private static Path writeFile(Path dir, String name, String content) throws IOException {
		Path file = dir.resolve(name);
		Files.writeString(file, content);
		return file;
	}

	/** Tests for the threaded indexing pipeline. */
	@Nested
	public class IndexingPipelineTests {
		/** Indexing a directory of files populates the shared index. */
		@Test
		public void testIndexDirectory(@TempDir Path dir) throws IOException {
			writeFile(dir, "one.txt", "apple banana cherry");
			writeFile(dir, "two.txt", "apple apple banana");

			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(3);
			try {
				ThreadFileIndexer.indexPath(dir, index, queue);
			} finally {
				queue.join();
			}

			assertTrue(index.containsStem("appl"));
			assertEquals(3, index.totalStems());
			assertEquals(2, index.totalLocations("appl"));
			assertEquals(3, index.getCount(dir.resolve("one.txt").toString()));
		}

		/** Non-text files are ignored during indexing. */
		@Test
		public void testIgnoreNonTextFiles(@TempDir Path dir) throws IOException {
			writeFile(dir, "keep.txt", "hello world");
			writeFile(dir, "skip.md", "ignored content");

			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(2);
			try {
				ThreadFileIndexer.indexPath(dir, index, queue);
			} finally {
				queue.join();
			}

			assertTrue(index.containsStem("hello"));
			assertFalse(index.containsStem("ignor"));
			assertEquals(1, index.totalCounts());
		}

		/** A single-file path indexes just that file. */
		@Test
		public void testIndexSingleFile(@TempDir Path dir) throws IOException {
			Path file = writeFile(dir, "solo.txt", "one two three");

			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(2);
			try {
				ThreadFileIndexer.indexPath(file, index, queue);
			} finally {
				queue.join();
			}

			assertEquals(3, index.totalStems());
			assertEquals(1, index.totalCounts());
		}
	}

	/** Tests for the threaded search pipeline against an indexed corpus. */
	@Nested
	public class SearchPipelineTests {
		/** Indexes a small corpus and returns the index plus a closed queue. */
		private ThreadSafeInvertedIndex indexCorpus(Path dir, WorkQueue queue) throws IOException {
			writeFile(dir, "fruits.txt", "apple apricot avocado");
			writeFile(dir, "more.txt", "apple banana");
			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			ThreadFileIndexer.indexPath(dir, index, queue);
			return index;
		}

		/** End-to-end exact search over a query file. */
		@Test
		public void testExactSearchPipeline(@TempDir Path dir) throws IOException {
			WorkQueue queue = new WorkQueue(3);
			try {
				ThreadSafeInvertedIndex index = indexCorpus(dir, queue);
				ThreadedSearchProcessor searcher = new ThreadedSearchProcessor(index, queue);

				Path queries = writeFile(dir, "q.txt", "apple");
				searcher.processFile(queries, false);

				List<SearchResult> results = searcher.getSearchResults("apple", false);
				assertEquals(2, results.size());
			} finally {
				queue.join();
			}
		}

		/** End-to-end partial search matches by prefix. */
		@Test
		public void testPartialSearchPipeline(@TempDir Path dir) throws IOException {
			WorkQueue queue = new WorkQueue(3);
			try {
				ThreadSafeInvertedIndex index = indexCorpus(dir, queue);
				ThreadedSearchProcessor searcher = new ThreadedSearchProcessor(index, queue);

				Path queries = writeFile(dir, "q.txt", "ap");
				searcher.processFile(queries, true);

				List<SearchResult> results = searcher.getSearchResults("ap", true);
				// fruits.txt: apple, apricot, avocado all start with "a"; "ap" matches
				// apple + apricot = 2 matches there, apple in more.txt = 1 match
				assertEquals(2, results.size());
				assertEquals(dir.resolve("fruits.txt").toString(), results.get(0).getLocation());
			} finally {
				queue.join();
			}
		}

		/** Duplicate query lines are only searched once. */
		@Test
		public void testDuplicateQueriesDeduplicated(@TempDir Path dir) throws IOException {
			WorkQueue queue = new WorkQueue(3);
			try {
				ThreadSafeInvertedIndex index = indexCorpus(dir, queue);
				ThreadedSearchProcessor searcher = new ThreadedSearchProcessor(index, queue);

				Path queries = writeFile(dir, "q.txt", "apple\napple\napple banana\nbanana apple");
				searcher.processFile(queries, false);

				// "apple banana" and "banana apple" stem+sort to the same key
				assertEquals(2, searcher.getQueries(false).size());
			} finally {
				queue.join();
			}
		}
	}

	/** Tests for JSON output of the index, counts, and results. */
	@Nested
	public class JsonOutputTests {
		/** Index and counts JSON files are written and non-empty. */
		@Test
		public void testWriteIndexAndCounts(@TempDir Path dir) throws IOException {
			writeFile(dir, "doc.txt", "alpha beta gamma");

			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(2);
			try {
				ThreadFileIndexer.indexPath(dir, index, queue);
			} finally {
				queue.join();
			}

			Path indexJson = dir.resolve("index.json");
			Path countsJson = dir.resolve("counts.json");
			index.indexJson(indexJson);
			index.countJson(countsJson);

			assertTrue(Files.exists(indexJson));
			assertTrue(Files.exists(countsJson));
			assertTrue(Files.readString(indexJson).contains("alpha"));
			assertTrue(Files.readString(countsJson).contains("doc.txt"));
		}

		/** Results JSON contains the queried term and a score field. */
		@Test
		public void testWriteResultsJson(@TempDir Path dir) throws IOException {
			writeFile(dir, "doc.txt", "alpha beta");

			WorkQueue queue = new WorkQueue(2);
			try {
				ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
				ThreadFileIndexer.indexPath(dir, index, queue);

				ThreadedSearchProcessor searcher = new ThreadedSearchProcessor(index, queue);
				Path queries = writeFile(dir, "q.txt", "alpha");
				searcher.processFile(queries, false);

				Path resultsJson = dir.resolve("results.json");
				searcher.searchJson(resultsJson, false);

				assertTrue(Files.exists(resultsJson));
				String content = Files.readString(resultsJson);
				assertTrue(content.contains("alpha"));
				assertTrue(content.contains("score"));
			} finally {
				queue.join();
			}
		}
	}

	/** Tests that the thread-safe and plain indexes produce identical results. */
	@Nested
	public class ConsistencyTests {
		/** Threaded indexing yields the same index as sequential indexing. */
		@Test
		public void testThreadedMatchesSequential(@TempDir Path dir) throws IOException {
			writeFile(dir, "a.txt", "the quick brown fox");
			writeFile(dir, "b.txt", "the lazy dog sleeps");
			writeFile(dir, "c.txt", "quick foxes and lazy dogs");

			// threaded build
			ThreadSafeInvertedIndex threaded = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(4);
			try {
				ThreadFileIndexer.indexPath(dir, threaded, queue);
			} finally {
				queue.join();
			}

			// sequential build using the same primitives
			InvertedIndex sequential = new InvertedIndex();
			for (Path file : List.of(dir.resolve("a.txt"), dir.resolve("b.txt"), dir.resolve("c.txt"))) {
				List<String> stems = FileStemmer.listStems(file);
				sequential.addAllWords(stems, file.toString(), 1);
			}

			assertEquals(sequential.getWords(), threaded.getWords());
			assertEquals(sequential.totalStems(), threaded.totalStems());
			for (String word : sequential.getWords()) {
				assertEquals(sequential.getLocations(word), threaded.getLocations(word));
			}
		}
	}
}
