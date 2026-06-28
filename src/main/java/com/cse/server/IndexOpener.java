package com.cse.server;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cse.cli.ArgumentParser;
import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.index.IndexStore;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.lucene.LuceneIndexStore;

/**
 * Opens or creates a Lucene index directory from CLI flags.
 */
public final class IndexOpener {
	private IndexOpener() {
	}

	public static Path resolveIndexDir(ArgumentParser parser) {
		if (parser.hasFlag("-index-dir") && parser.hasValue("-index-dir")) {
			return parser.getPath("-index-dir", IndexBuilder.DEFAULT_INDEX_DIR);
		}
		return IndexBuilder.DEFAULT_INDEX_DIR;
	}

	public static boolean shouldLoadExisting(ArgumentParser parser, Path indexDir) {
		if (parser.hasFlag("-load-index")) {
			return true;
		}
		return Files.isDirectory(indexDir) && hasLuceneSegments(indexDir);
	}

	public static IndexStore open(ArgumentParser parser) throws IOException {
		Path indexDir = resolveIndexDir(parser);
		LuceneIndexStore store = new LuceneIndexStore();
		store.open(indexDir);
		return store;
	}

	/**
	 * Opens an existing Lucene index or builds from {@code -text} / {@code -html} when needed.
	 */
	public static IndexStore openOrBuild(ArgumentParser parser, int threads) throws Exception {
		Path indexDir = resolveIndexDir(parser);
		LuceneIndexStore store = new LuceneIndexStore();
		store.open(indexDir);
		if (shouldLoadExisting(parser, indexDir)) {
			return store;
		}
		WorkQueue queue = new WorkQueue(threads);
		try {
			if (parser.hasFlag("-text") && parser.hasValue("-text")) {
				Path path = parser.getPath("-text", Path.of(""));
				ThreadFileIndexer.indexPath(path, store, queue);
			}
			if (parser.hasFlag("-html")) {
				String seed = parser.getString("-html");
				if (seed != null && !seed.isBlank()) {
					URI seedUri = URI.create(seed);
					int crawlLimit = parser.hasFlag("-crawl") ? parser.getInteger("-crawl", 1) : 1;
					if (crawlLimit <= 0) {
						crawlLimit = 1;
					}
					WebCrawler crawler = new WebCrawler(store, queue, crawlLimit, store.listLocations(), null);
					crawler.crawl(seedUri);
				}
			}
			store.commit();
		} finally {
			queue.shutdown();
			queue.join();
		}
		return store;
	}

	private static boolean hasLuceneSegments(Path indexDir) {
		try (var stream = Files.list(indexDir)) {
			return stream.anyMatch(p -> p.getFileName().toString().startsWith("segments_"));
		} catch (IOException e) {
			return false;
		}
	}
}
