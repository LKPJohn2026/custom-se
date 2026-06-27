package com.cse.ai.http;

import java.time.Duration;

/**
 * Configurable HTTP timeouts and retry policy for AI providers.
 */
public record AiHttpConfig(int connectTimeoutMs, int readTimeoutMs, int maxRetries) {

	public static AiHttpConfig defaults() {
		return new AiHttpConfig(5_000, 120_000, 2);
	}

	public Duration connectTimeout() {
		return Duration.ofMillis(connectTimeoutMs);
	}

	public Duration readTimeout() {
		return Duration.ofMillis(readTimeoutMs);
	}
}
