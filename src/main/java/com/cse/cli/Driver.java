package com.cse.cli;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import com.cse.ai.config.AiConfigException;
import com.cse.ai.config.AiConfigValidator;
import com.cse.ai.config.EnvFileLoader;
import com.cse.ai.embed.EmbeddingIndexJob;
import com.cse.ai.profile.AiPreferences;
import com.cse.ai.profile.AiProfile;
import com.cse.ai.profile.AiProfileResolver;
import com.cse.ai.profile.AiSettings;
import com.cse.ai.rag.RagResponse;
import com.cse.ai.rag.RagService;
import com.cse.benchmark.BenchmarkMain;
import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.index.IndexStore;
import com.cse.index.ThreadFileIndexer;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.index.lucene.LuceneIndexStore;
import com.cse.search.Searcher;
import com.cse.search.ThreadedSearchProcessor;
import com.cse.server.AppContext;
import com.cse.server.IndexBuilder;
import com.cse.server.IndexOpener;
import com.cse.server.JettyServer;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Phong La
 */
public class Driver {
	private static final int DEFAULT_SERVER_PORT = 8080;

	private static int parseServerPort(ArgumentParser parser) {
		if (!parser.hasFlag("-server")) {
			return -1;
		}

		String value = parser.getString("-server");
		if (value == null || value.isBlank()) {
			return DEFAULT_SERVER_PORT;
		}

		try {
			int port = Integer.parseInt(value);
			return port > 0 ? port : DEFAULT_SERVER_PORT;
		} catch (NumberFormatException e) {
			return DEFAULT_SERVER_PORT;
		}
	}

	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		WorkQueue queue = null;

		try {
			ArgumentParser parser = new ArgumentParser(args);
			boolean partial = parser.hasFlag("-partial");
			int serverPort = parseServerPort(parser);

			int threads = parser.getInteger("-threads", 5);
			if (threads <= 0) {
				threads = 5;
			}

			ThreadSafeInvertedIndex index = new ThreadSafeInvertedIndex();
			queue = new WorkQueue(threads);
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
						int crawlLimit = parser.hasFlag("-crawl") ? parser.getInteger("-crawl", 1) : 1;
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

			if (queue != null) {
				queue.shutdown();
				queue.join();
				queue = null;
			}

			if (parser.hasFlag("-reindex-embeddings")) {
				runReindexEmbeddings(parser);
			}

			if (parser.hasFlag("-ask") && parser.hasValue("-ask")) {
				runAsk(parser);
			}

			if (parser.hasFlag("-benchmark")) {
				BenchmarkMain.run(parser);
			}

			if (serverPort > 0) {
				try {
					AiSettings aiSettings = loadValidatedStack(parser, null);
					IndexStore serverIndex = buildServerIndex(parser, threads);
					AppContext ctx = new AppContext(serverIndex, threads);
					JettyServer server = new JettyServer(serverPort, ctx);
					server.startAndJoin();
				} catch (AiConfigException e) {
					System.out.println(e.getMessage());
				} catch (Exception e) {
					System.out.println("Unable to start the web server.");
				}
			}
		} catch (Exception e) {
			System.out.println("Unable to run the search engine.");
		} finally {
			if (queue != null) {
				queue.shutdown();
				queue.join();
			}
		}
	}

	private static IndexStore buildServerIndex(ArgumentParser parser, int threads) throws Exception {
		Path indexDir = IndexOpener.resolveIndexDir(parser);
		LuceneIndexStore store = new LuceneIndexStore();
		store.open(indexDir);
		if (IndexOpener.shouldLoadExisting(parser, indexDir)) {
			return store;
		}
		WorkQueue sq = new WorkQueue(threads);
		try {
			if (parser.hasFlag("-text") && parser.hasValue("-text")) {
				Path path = parser.getPath("-text", Path.of(""));
				ThreadFileIndexer.indexPath(path, store, sq);
			}
			if (parser.hasFlag("-html")) {
				String seed = parser.getString("-html");
				if (seed != null && !seed.isBlank()) {
					URI seedUri = URI.create(seed);
					int crawlLimit = parser.hasFlag("-crawl") ? parser.getInteger("-crawl", 1) : 1;
					if (crawlLimit <= 0) {
						crawlLimit = 1;
					}
					WebCrawler crawler = new WebCrawler(store, sq, crawlLimit, store.listLocations(), null);
					crawler.crawl(seedUri);
				}
			}
			store.commit();
		} finally {
			sq.shutdown();
			sq.join();
		}
		return store;
	}

	private static void runAsk(ArgumentParser parser) {
		try (IndexStore store = IndexOpener.open(parser)) {
			AiSettings settings = loadValidatedStack(parser, null);
			AiProfileResolver resolver = new AiProfileResolver(settings);
			String stackId = parser.getString("-ai-stack", settings.defaultStack());
			AiProfile profile = resolver.resolve(AiPreferences.defaults(stackId));
			RagService ragService = new RagService(store, settings);
			RagResponse response = ragService.ask(parser.getString("-ask"), profile, null);
			System.out.println(response.answer());
			response.sources().forEach(s -> System.out.println("  - " + s.chunk().location()));
		} catch (AiConfigException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println("Unable to run ask.");
		}
	}

	private static void runReindexEmbeddings(ArgumentParser parser) {
		try (IndexStore store = IndexOpener.open(parser)) {
			AiSettings settings = loadValidatedStack(parser, null);
			AiProfileResolver resolver = new AiProfileResolver(settings);
			String stackId = parser.getString("-ai-stack", settings.defaultStack());
			AiProfile profile = resolver.resolve(AiPreferences.defaults(stackId));
			int count = EmbeddingIndexJob.runSync(store, profile.embeddings());
			System.out.println("Re-embedded " + count + " chunks.");
		} catch (AiConfigException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println("Unable to re-embed chunks.");
		}
	}

	private static AiSettings loadValidatedStack(ArgumentParser parser, String overrideStack) {
		EnvFileLoader.loadRequired();
		AiSettings settings = AiSettings.load();
		String stackId = overrideStack != null ? overrideStack
				: parser.getString("-ai-stack", settings.defaultStack());
		AiConfigValidator.validate(settings, stackId);
		return settings;
	}
}
