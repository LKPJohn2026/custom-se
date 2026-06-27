package com.cse.ai.config;

import java.net.URI;

import com.cse.ai.profile.AiSettings;

/**
 * Validates {@code .env} values required for the active AI stack.
 */
public final class AiConfigValidator {
	private AiConfigValidator() {
	}

	public static void validate(AiSettings settings, String stackId) {
		switch (stackId) {
			case "openai" -> requireOpenAiKey(settings.openAiApiKey());
			case "lmstudio" -> requireUrl("AI_LMSTUDIO_BASE_URL", settings.lmstudioBaseUrl());
			case "claude" -> {
				requireAnthropicKey(settings.anthropicApiKey());
				validateClaudeEmbeddings(settings);
			}
			default -> requireUrl("OLLAMA_BASE_URL", settings.ollamaBaseUrl());
		}
	}

	private static void validateClaudeEmbeddings(AiSettings settings) {
		switch (settings.claudeEmbeddingStack()) {
			case "voyage" -> requireVoyageKey(settings.voyageApiKey());
			case "openai" -> requireOpenAiKey(settings.openAiApiKey());
			default -> requireUrl("OLLAMA_BASE_URL", settings.ollamaBaseUrl());
		}
	}

	private static void requireOpenAiKey(String key) {
		if (key.isBlank()) {
			throw new AiConfigException("OPENAI_API_KEY is required in .env for the OpenAI stack.");
		}
		if (!key.startsWith("sk-") || key.length() < 20) {
			throw new AiConfigException("OPENAI_API_KEY in .env must start with sk- and be a valid API key.");
		}
	}

	private static void requireAnthropicKey(String key) {
		if (key.isBlank()) {
			throw new AiConfigException("ANTHROPIC_API_KEY is required in .env for the Claude stack.");
		}
		if (!key.startsWith("sk-ant-") || key.length() < 20) {
			throw new AiConfigException("ANTHROPIC_API_KEY in .env must start with sk-ant- and be a valid API key.");
		}
	}

	private static void requireVoyageKey(String key) {
		if (key.isBlank()) {
			throw new AiConfigException("VOYAGE_API_KEY is required in .env for Claude + Voyage embeddings.");
		}
		if (!key.startsWith("pa-") || key.length() < 20) {
			throw new AiConfigException("VOYAGE_API_KEY in .env must start with pa- and be a valid API key.");
		}
	}

	private static void requireUrl(String name, String url) {
		if (url.isBlank()) {
			throw new AiConfigException(name + " is required in .env.");
		}
		try {
			URI uri = URI.create(url);
			if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
				throw new AiConfigException(name + " in .env must be an http or https URL.");
			}
		} catch (IllegalArgumentException e) {
			throw new AiConfigException(name + " in .env is not a valid URL: " + url);
		}
	}
}
