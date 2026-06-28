package com.cse.benchmark;

import java.nio.file.Path;

import com.cse.cli.ArgumentParser;

/**
 * CLI-derived settings for {@link BenchmarkRunner}.
 */
public record BenchmarkConfig(
		Path queryFile,
		Path corpusPath,
		int warmup,
		int iterations,
		boolean partial,
		int limit) {

	public static final int DEFAULT_WARMUP = 20;
	public static final int DEFAULT_ITERATIONS = 100;
	public static final int DEFAULT_LIMIT = 50;

	public BenchmarkConfig {
		if (warmup < 0) {
			warmup = 0;
		}
		if (iterations <= 0) {
			iterations = DEFAULT_ITERATIONS;
		}
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
	}

	public static BenchmarkConfig from(ArgumentParser parser) {
		Path queryFile = parser.hasFlag("-benchmark-queries") && parser.hasValue("-benchmark-queries")
				? parser.getPath("-benchmark-queries")
				: null;
		Path corpusPath = parser.hasFlag("-corpus") && parser.hasValue("-corpus")
				? parser.getPath("-corpus")
				: null;
		int warmup = parser.getInteger("-warmup", DEFAULT_WARMUP);
		int iterations = parser.getInteger("-iterations", DEFAULT_ITERATIONS);
		boolean partial = parser.hasFlag("-partial");
		int limit = parser.getInteger("-limit", DEFAULT_LIMIT);
		return new BenchmarkConfig(queryFile, corpusPath, warmup, iterations, partial, limit);
	}
}
