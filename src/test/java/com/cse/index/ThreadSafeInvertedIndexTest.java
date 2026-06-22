package com.cse.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.cse.concurrent.WorkQueue;
import com.cse.stem.FileStemmer;

/**
 * Concurrency tests for {@link ThreadSafeInvertedIndex}.
 */
public class ThreadSafeInvertedIndexTest {

	/**
	 * Many threads merging local indexes in parallel produce correct totals.
	 */
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testConcurrentAddAll() throws InterruptedException {
		ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
		int threads = 16;
		int wordsPerThread = 50;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch done = new CountDownLatch(threads);

		for (int t = 0; t < threads; t++) {
			final int id = t;
			pool.submit(() -> {
				try {
					InvertedIndex local = new InvertedIndex();
					for (int i = 0; i < wordsPerThread; i++) {
						local.addWord("word" + i, "file" + id + ".txt", i + 1);
					}
					index.addAll(local);
				} finally {
					done.countDown();
				}
			});
		}

		done.await(20, TimeUnit.SECONDS);
		pool.shutdown();

		assertEquals(threads, index.totalCounts());
		assertEquals(wordsPerThread, index.totalStems());
		assertTrue(index.containsStem("word0"));
		assertTrue(index.containsLocation("word0", "file0.txt"));
	}

	/**
	 * Parallel file indexing via {@link ThreadFileIndexer} matches a sequential build.
	 */
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testConcurrentFileIndexing(@TempDir Path dir) throws IOException {
		for (int i = 0; i < 12; i++) {
			Files.writeString(dir.resolve("doc" + i + ".txt"), "token" + i + " shared");
		}

		ThreadSafeInvertedIndex threaded = new ThreadSafeInvertedIndex();
		WorkQueue queue = new WorkQueue(6);
		try {
			ThreadFileIndexer.indexPath(dir, threaded, queue);
		} finally {
			queue.join();
		}

		InvertedIndex sequential = new InvertedIndex();
		for (int i = 0; i < 12; i++) {
			Path file = dir.resolve("doc" + i + ".txt");
			sequential.addAllWords(FileStemmer.listStems(file), file.toString(), 1);
		}

		assertEquals(sequential.getWords(), threaded.getWords());
		assertEquals(sequential.totalStems(), threaded.totalStems());
		assertTrue(threaded.containsStem("share"));
	}

	/**
	 * Concurrent searches during indexing complete without errors.
	 */
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	public void testConcurrentSearchDuringIndexing(@TempDir Path dir) throws Exception {
		for (int i = 0; i < 8; i++) {
			Files.writeString(dir.resolve("f" + i + ".txt"), "searchterm content " + i);
		}

		ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
		WorkQueue queue = new WorkQueue(4);
		ExecutorService searchers = Executors.newFixedThreadPool(4);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger errors = new AtomicInteger();

		for (int i = 0; i < 4; i++) {
			searchers.submit(() -> {
				try {
					start.await();
					for (int r = 0; r < 50; r++) {
						index.totalStems();
						index.exactIndex(Set.of("searchterm"));
						index.partialIndex(Set.of("search"));
					}
				} catch (Exception e) {
					errors.incrementAndGet();
				}
			});
		}

		start.countDown();
		try {
			ThreadFileIndexer.indexPath(dir, index, queue);
		} finally {
			searchers.shutdown();
			searchers.awaitTermination(10, TimeUnit.SECONDS);
			queue.join();
		}

		assertEquals(0, errors.get());
		assertTrue(index.containsStem("searchterm"));
	}
}
