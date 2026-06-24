package com.cse.server;

import com.cse.concurrent.WorkQueue;
import com.cse.index.IndexStore;
import com.cse.search.SearchEngine;
import com.cse.server.meta.MetadataStore;

/**
 * Application-wide state shared by servlets during server runtime.
 */
public class AppContext {
	public static final String ATTR = "appContext";
	public static final String ADMIN_PASSWORD = "admin";

	private final IndexStore index;
	private final SearchEngine searchEngine;
	private final MetadataStore metadata;
	private final ServerStats stats;
	private final int threads;
	private final ServerSettings settings;
	private final CrawlJobManager crawlJobs;
	private final RateLimiter searchRateLimiter;
	private volatile Runnable shutdownHook;

	public AppContext(IndexStore index, int threads) {
		this(index, threads, ServerSettings.load());
	}

	public AppContext(IndexStore index, int threads, ServerSettings settings) {
		this.index = index;
		this.metadata = new MetadataStore();
		this.stats = new ServerStats();
		this.threads = threads;
		this.settings = settings;
		this.crawlJobs = new CrawlJobManager();
		this.searchRateLimiter = new RateLimiter(120);
		this.searchEngine = new SearchEngine(index, metadata, stats);
	}

	public IndexStore index() {
		return index;
	}

	public SearchEngine searchEngine() {
		return searchEngine;
	}

	public MetadataStore metadata() {
		return metadata;
	}

	public ServerStats stats() {
		return stats;
	}

	public int threads() {
		return threads;
	}

	public ServerSettings settings() {
		return settings;
	}

	public CrawlJobManager crawlJobs() {
		return crawlJobs;
	}

	public RateLimiter searchRateLimiter() {
		return searchRateLimiter;
	}

	public WorkQueue newWorkQueue() {
		return new WorkQueue(threads);
	}

	public void setShutdownHook(Runnable shutdownHook) {
		this.shutdownHook = shutdownHook;
	}

	public void shutdown() {
		Runnable hook = shutdownHook;
		if (hook != null) {
			hook.run();
		}
	}
}
