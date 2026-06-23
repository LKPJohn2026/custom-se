package com.cse.server;

import com.cse.concurrent.WorkQueue;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.server.meta.MetadataStore;

/**
 * Application-wide state shared by servlets during server runtime.
 */
public class AppContext {
	public static final String ATTR = "appContext";
	public static final String ADMIN_PASSWORD = "admin";

	private final ThreadSafeInvertedIndex index;
	private final MetadataStore metadata;
	private final ServerStats stats;
	private final int threads;
	private volatile Runnable shutdownHook;

	public AppContext(ThreadSafeInvertedIndex index, int threads) {
		this.index = index;
		this.metadata = new MetadataStore();
		this.stats = new ServerStats();
		this.threads = threads;
	}

	public ThreadSafeInvertedIndex index() {
		return index;
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
