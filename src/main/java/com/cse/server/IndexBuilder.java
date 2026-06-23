package com.cse.server;

import java.io.IOException;

import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.ThreadSafeInvertedIndex;

public class IndexBuilder {
	public static ThreadSafeInvertedIndex build(ServerConfig config) throws IOException {
		ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
		WorkQueue queue = new WorkQueue(config.threads);

		try {
			if (config.textPath != null) {
				ThreadFileIndexer.indexPath(config.textPath, index, queue);
			} else if (config.htmlSeed != null) {
				WebCrawler crawler = new WebCrawler(index, queue, config.crawlLimit);
				crawler.crawl(config.htmlSeed);
			} else {
				throw new IllegalArgumentException("Missing index source.");
			}
		} finally {
			queue.shutdown();
			queue.join();
		}

		return index;
	}
}

