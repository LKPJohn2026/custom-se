package com.cse.ai.embed;

/**
 * Embedding provider failure surfaced to callers without stack traces in UI layers.
 */
public class EmbeddingException extends RuntimeException {
	public EmbeddingException(String message) {
		super(message);
	}

	public EmbeddingException(String message, Throwable cause) {
		super(message, cause);
	}
}
