package com.cse.benchmark;

import java.nio.file.Path;

/**
 * Aggregated benchmark output for keyword search.
 */
public record BenchmarkReport(
		long documents,
		long chunks,
		long corpusBytes,
		long indexBytes,
		LatencyStats latency,
		HardwareInfo hardware,
		boolean partial,
		int queryCount) {

	public String oneLiner() {
		String corpus = HardwareInfo.formatBytes(corpusBytes);
		String index = HardwareInfo.formatBytes(indexBytes);
		return String.format(
				"Indexed %,d documents / %s corpus (%s on-disk index, %,d chunks) with %d ms median query latency on %s.",
				documents,
				corpus,
				index,
				chunks,
				latency.medianMs(),
				hardware.summary());
	}

	public String details() {
		return String.format(
				"queries=%d partial=%s samples=%d min=%dms median=%dms p95=%dms max=%dms",
				queryCount,
				partial,
				latency.count(),
				latency.minMs(),
				latency.medianMs(),
				latency.p95Ms(),
				latency.maxMs());
	}

	public static long directorySize(Path dir) {
		if (dir == null || !java.nio.file.Files.isDirectory(dir)) {
			return 0;
		}
		try (var walk = java.nio.file.Files.walk(dir)) {
			return walk.filter(java.nio.file.Files::isRegularFile)
					.mapToLong(p -> {
						try {
							return java.nio.file.Files.size(p);
						} catch (java.io.IOException e) {
							return 0L;
						}
					})
					.sum();
		} catch (java.io.IOException e) {
			return 0;
		}
	}
}
