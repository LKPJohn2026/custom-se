package com.cse.ai.llm;

import java.util.List;
import java.util.stream.Stream;

/**
 * Chat completion client for RAG answer synthesis.
 */
public interface LlmClient {

	String providerId();

	String model();

	Stream<String> streamChat(ChatRequest request);

	String completeChat(ChatRequest request);
}
