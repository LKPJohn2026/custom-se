package com.cse.ai.rag;

import java.util.List;

/**
 * Builds grounded prompts for {@link RagService}.
 */
public final class PromptBuilder {
	private static final String SYSTEM = """
			You are a search assistant. Answer using ONLY the provided sources.
			If the sources do not contain enough information, say so clearly.
			Cite source URLs when making claims.
			""";

	private PromptBuilder() {
	}

	public static String systemPrompt() {
		return SYSTEM.strip();
	}

	public static String userPrompt(List<ScoredChunk> sources, String question, int maxContextChars) {
		StringBuilder sb = new StringBuilder();
		sb.append("Sources:\n");
		int used = 0;
		for (ScoredChunk source : sources) {
			String block = source.chunk().location() + ": " + source.chunk().text() + "\n\n";
			if (used + block.length() > maxContextChars) {
				break;
			}
			sb.append(block);
			used += block.length();
		}
		sb.append("Question: ").append(question);
		return sb.toString();
	}
}
