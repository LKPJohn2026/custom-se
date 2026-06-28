package com.cse.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class LatencyStatsTest {
	@Test
	public void testPercentiles() {
		LatencyStats stats = LatencyStats.fromMillis(List.of(1L, 2L, 3L, 4L, 100L));
		assertEquals(1, stats.minMs());
		assertEquals(3, stats.medianMs());
		assertEquals(100, stats.p95Ms());
		assertEquals(100, stats.maxMs());
		assertEquals(5, stats.count());
	}

	@Test
	public void testEmpty() {
		LatencyStats stats = LatencyStats.fromMillis(List.of());
		assertEquals(0, stats.medianMs());
		assertEquals(0, stats.count());
	}
}
