package com.cse.ai.profile;

import com.cse.ai.embed.EmbeddingProvider;
import com.cse.ai.llm.LlmClient;

/**
 * Active AI stack: embedding provider + chat client.
 */
public record AiProfile(
		String id,
		String displayName,
		EmbeddingProvider embeddings,
		LlmClient chat,
		boolean mixedStack) {
}
