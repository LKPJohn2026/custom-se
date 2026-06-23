package com.cse.server;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks server-wide runtime statistics.
 */
public class ServerStats {
	private final Instant start = Instant.now();
	private final AtomicLong totalQueries = new AtomicLong();

	public Instant startTime() {
		return start;
	}

	public long totalQueries() {
		return totalQueries.get();
	}

	public void recordQuery() {
		totalQueries.incrementAndGet();
	}

	public String uptime() {
		Duration d = Duration.between(start, Instant.now());
		long hours = d.toHours();
		long minutes = d.toMinutesPart();
		long seconds = d.toSecondsPart();
		return String.format("%dh %dm %ds", hours, minutes, seconds);
	}
}
