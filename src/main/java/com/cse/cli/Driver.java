package com.cse.cli;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.search.Searcher;
import com.cse.search.ThreadedSearchProcessor;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Phong La
 */
public class Driver {
	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		ArgumentParser parser = new ArgumentParser(args);
		boolean partial = parser.hasFlag("-partial");

		int threads = parser.getInteger("-threads", 5);
		if (threads <= 0) {
			threads = 5;
		}

		ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
		WorkQueue queue = new WorkQueue(threads);
		Searcher searcher = new ThreadedSearchProcessor(index, queue);

		if (parser.hasFlag("-text") && parser.hasValue("-text")) {
			Path path = parser.getPath("-text", Path.of(""));
			try {
				ThreadFileIndexer.indexPath(path, index, queue);
			} catch (IOException e) {
				System.out.println("Unable to index text file: " + path);
			}
		}

		if (parser.hasFlag("-html")) {
			String seed = parser.getString("-html");
			try {
				if (seed != null && !seed.isBlank()) {
					URI seedUri = URI.create(seed);
					int crawlLimit = parser.hasFlag("-crawl")
							? parser.getInteger("-crawl", 1)
							: 1;
					if (crawlLimit <= 0) {
						crawlLimit = 1;
					}
					WebCrawler crawler = new WebCrawler(index, queue, crawlLimit);
					crawler.crawl(seedUri);
				} else {
					System.out.println("No seed URI provided for web crawling.");
				}
			} catch (Exception e) {
				System.out.println("Unable to crawl the web.");
			}
		}

		if (parser.hasFlag("-query") && parser.hasValue("-query")) {
			Path queryPath = parser.getPath("-query", Path.of(""));
			try {
				searcher.processFile(queryPath, partial);
			} catch (IOException e) {
				System.out.println("Unable to search on the input file.");
			}
		}

		if (parser.hasFlag("-counts")) {
			Path countPath = parser.getPath("-counts", Path.of("counts.json"));
			try {
				index.countJson(countPath);
			} catch (IOException e) {
				System.out.println("Unable to write counts at path: " + countPath);
			}
		}

		if (parser.hasFlag("-index")) {
			Path indexPath = parser.getPath("-index", Path.of("index.json"));
			try {
				index.indexJson(indexPath);
			} catch (IOException e) {
				System.out.println("Unable to write index at path: " + indexPath);
			}
		}

		if (parser.hasFlag("-results")) {
			Path searchPath = parser.getPath("-results", Path.of("results.json"));
			try {
				searcher.searchJson(searchPath, partial);
			} catch (IOException e) {
				System.out.println("Unable to write to the output file.");
			}
		}

		queue.shutdown();
		queue.join();
	}
}
