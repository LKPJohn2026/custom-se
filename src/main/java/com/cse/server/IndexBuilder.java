package com.cse.server;

import java.io.IOException;
import java.nio.file.Path;

import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.index.IndexStore;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.lucene.LuceneIndexStore;

public class IndexBuilder {
	public static final Path DEFAULT_INDEX_DIR = Path.of("data", "index");

	public static IndexStore build(ServerConfig config) throws IOException {
		return build(config, DEFAULT_INDEX_DIR);
	}

	public static IndexStore build(ServerConfig config, Path indexDir) throws IOException {
		return build(config, indexDir, false);
	}

	public static IndexStore build(ServerConfig config, Path indexDir, boolean loadOnly) throws IOException {
		LuceneIndexStore store = new LuceneIndexStore();
		store.open(indexDir);
		if (loadOnly) {
			return store;
		}
		WorkQueue queue = new WorkQueue(config.threads);

		try {
			if (config.textPath != null) {
				ThreadFileIndexer.indexPath(config.textPath, store, queue);
			} else if (config.htmlSeed != null) {
				WebCrawler crawler = new WebCrawler(store, queue, config.crawlLimit, store.listLocations(), null);
				crawler.crawl(config.htmlSeed);
			} else {
				throw new IllegalArgumentException("Missing index source.");
			}
			store.commit();
		} finally {
			queue.shutdown();
			queue.join();
		}

		return store;
	}
}
