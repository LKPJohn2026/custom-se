package com.cse.ai.llm;

/**
 * LLM provider failure surfaced without stack traces in user-visible output.
 */
public class LlmException extends RuntimeException {
	public LlmException(String message) {
		super(message);
	}

	public LlmException(String message, Throwable cause) {
		super(message, cause);
	}
}
