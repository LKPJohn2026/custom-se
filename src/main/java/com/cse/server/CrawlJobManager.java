package com.cse.server;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks a background crawl job.
 */
public final class CrawlJobManager {
	public record JobState(String status, int crawled, int max, String message) {
	}

	private final AtomicReference<JobState> state = new AtomicReference<>(
			new JobState("idle", 0, 0, ""));

	public JobState state() {
		return state.get();
	}

	public boolean isRunning() {
		return "running".equals(state.get().status());
	}

	public void start(int max) {
		state.set(new JobState("running", 0, max, "Crawl started"));
	}

	public void finish(int crawled, String message) {
		state.set(new JobState("done", crawled, state.get().max(), message));
	}

	public void fail(String message) {
		state.set(new JobState("failed", state.get().crawled(), state.get().max(), message));
	}
}
