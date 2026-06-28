package com.cse.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Percentile statistics over search latency samples (milliseconds).
 */
public final class LatencyStats {
	private final long minMs;
	private final long medianMs;
	private final long p95Ms;
	private final long maxMs;
	private final int count;

	private LatencyStats(long minMs, long medianMs, long p95Ms, long maxMs, int count) {
		this.minMs = minMs;
		this.medianMs = medianMs;
		this.p95Ms = p95Ms;
		this.maxMs = maxMs;
		this.count = count;
	}

	public static LatencyStats fromMillis(List<Long> samples) {
		if (samples == null || samples.isEmpty()) {
			return new LatencyStats(0, 0, 0, 0, 0);
		}
		List<Long> sorted = new ArrayList<>(samples);
		Collections.sort(sorted);
		return new LatencyStats(
				sorted.get(0),
				percentile(sorted, 50),
				percentile(sorted, 95),
				sorted.get(sorted.size() - 1),
				sorted.size());
	}

	private static long percentile(List<Long> sorted, int pct) {
		int index = Math.min(sorted.size() - 1, (int) Math.ceil(pct / 100.0 * sorted.size()) - 1);
		return sorted.get(Math.max(0, index));
	}

	public long minMs() {
		return minMs;
	}

	public long medianMs() {
		return medianMs;
	}

	public long p95Ms() {
		return p95Ms;
	}

	public long maxMs() {
		return maxMs;
	}

	public int count() {
		return count;
	}
}
