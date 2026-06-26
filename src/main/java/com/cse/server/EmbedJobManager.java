package com.cse.server;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks a background embedding re-index job.
 */
public final class EmbedJobManager {
	public record JobState(String status, int processed, int total, String message) {
	}

	private final AtomicReference<JobState> state = new AtomicReference<>(
			new JobState("idle", 0, 0, ""));

	public JobState state() {
		return state.get();
	}

	public boolean isRunning() {
		return "running".equals(state.get().status());
	}

	public void start(int total) {
		state.set(new JobState("running", 0, total, "Re-embedding started"));
	}

	public void progress(int processed) {
		JobState current = state.get();
		state.set(new JobState("running", processed, current.total(), current.message()));
	}

	public void finish(int processed, String message) {
		state.set(new JobState("done", processed, state.get().total(), message));
	}

	public void fail(String message) {
		state.set(new JobState("failed", state.get().processed(), state.get().total(), message));
	}
}
