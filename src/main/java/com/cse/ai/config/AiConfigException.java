package com.cse.ai.config;

/**
 * Raised when required AI configuration is missing or invalid.
 */
public final class AiConfigException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AiConfigException(String message) {
		super(message);
	}
}
