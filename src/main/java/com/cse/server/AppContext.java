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
	private volatile Runnable shutdownHook;

	public AppContext(IndexStore index, int threads) {
		this.index = index;
		this.metadata = new MetadataStore();
		this.stats = new ServerStats();
		this.threads = threads;
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
