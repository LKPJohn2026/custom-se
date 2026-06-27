package com.cse.ai.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.cse.ai.config.EnvFileLoader;

/**
 * AI provider configuration from {@code application.properties} and {@code .env}.
 */
public final class AiSettings {
	private final String defaultStack;
	private final String ollamaBaseUrl;
	private final String ollamaEmbeddingModel;
	private final String ollamaChatModel;
	private final int ollamaEmbeddingDimensions;
	private final String lmstudioBaseUrl;
	private final String lmstudioEmbeddingModel;
	private final String lmstudioChatModel;
	private final int lmstudioEmbeddingDimensions;
	private final String openaiEmbeddingModel;
	private final String openaiChatModel;
	private final int openaiEmbeddingDimensions;
	private final String claudeChatModel;
	private final String claudeEmbeddingStack;
	private final String voyageEmbeddingModel;
	private final int voyageEmbeddingDimensions;
	private final int ragTopK;
	private final int ragMaxContextTokens;
	private final int askRateLimitPerMinute;

	private AiSettings(Properties props) {
		this.defaultStack = strProp(props, "ai.defaultStack", "AI_DEFAULT_STACK", "ollama");
		this.ollamaBaseUrl = strProp(props, "ai.ollama.baseUrl", "OLLAMA_BASE_URL", "http://localhost:11434");
		this.ollamaEmbeddingModel = strProp(props, "ai.ollama.embeddingModel", "OLLAMA_EMBEDDING_MODEL",
				"nomic-embed-text");
		this.ollamaChatModel = strProp(props, "ai.ollama.chatModel", "OLLAMA_CHAT_MODEL", "llama3.2");
		this.ollamaEmbeddingDimensions = intProp(props, "ai.ollama.embeddingDimensions", "OLLAMA_EMBEDDING_DIMENSIONS",
				768);
		this.lmstudioBaseUrl = strProp(props, "ai.lmstudio.baseUrl", "AI_LMSTUDIO_BASE_URL",
				"http://localhost:1234/v1");
		this.lmstudioEmbeddingModel = strProp(props, "ai.lmstudio.embeddingModel", "LMSTUDIO_EMBEDDING_MODEL", "");
		this.lmstudioChatModel = strProp(props, "ai.lmstudio.chatModel", "LMSTUDIO_CHAT_MODEL", "");
		this.lmstudioEmbeddingDimensions = intProp(props, "ai.lmstudio.embeddingDimensions",
				"LMSTUDIO_EMBEDDING_DIMENSIONS", 768);
		this.openaiEmbeddingModel = strProp(props, "ai.openai.embeddingModel", "OPENAI_EMBEDDING_MODEL",
				"text-embedding-3-small");
		this.openaiChatModel = strProp(props, "ai.openai.chatModel", "OPENAI_CHAT_MODEL", "gpt-4o-mini");
		this.openaiEmbeddingDimensions = intProp(props, "ai.openai.embeddingDimensions", "OPENAI_EMBEDDING_DIMENSIONS",
				1536);
		this.claudeChatModel = strProp(props, "ai.claude.chatModel", "CLAUDE_CHAT_MODEL",
				"claude-sonnet-4-20250514");
		this.claudeEmbeddingStack = strProp(props, "ai.claude.embeddingStack", "CLAUDE_EMBEDDING_STACK", "voyage");
		this.voyageEmbeddingModel = strProp(props, "ai.voyage.embeddingModel", "VOYAGE_EMBEDDING_MODEL", "voyage-4");
		this.voyageEmbeddingDimensions = intProp(props, "ai.voyage.embeddingDimensions", "VOYAGE_EMBEDDING_DIMENSIONS",
				1024);
		this.ragTopK = intProp(props, "ai.rag.topK", "AI_RAG_TOP_K", 8);
		this.ragMaxContextTokens = intProp(props, "ai.rag.maxContextTokens", "AI_RAG_MAX_CONTEXT_TOKENS", 6000);
		this.askRateLimitPerMinute = intProp(props, "ai.ask.rateLimitPerMinute", "AI_ASK_RATE_LIMIT_PER_MINUTE", 30);
	}

	public static AiSettings load() {
		EnvFileLoader.loadOptional();
		Properties props = new Properties();
		try (InputStream in = AiSettings.class.getResourceAsStream("/application.properties")) {
			if (in != null) {
				props.load(in);
			}
		} catch (IOException ignored) {
		}
		return new AiSettings(props);
	}

	public String defaultStack() {
		return defaultStack;
	}

	public String ollamaBaseUrl() {
		return ollamaBaseUrl;
	}

	public String ollamaEmbeddingModel() {
		return ollamaEmbeddingModel;
	}

	public String ollamaChatModel() {
		return ollamaChatModel;
	}

	public int ollamaEmbeddingDimensions() {
		return ollamaEmbeddingDimensions;
	}

	public String lmstudioBaseUrl() {
		return lmstudioBaseUrl;
	}

	public String lmstudioEmbeddingModel() {
		return lmstudioEmbeddingModel;
	}

	public String lmstudioChatModel() {
		return lmstudioChatModel;
	}

	public int lmstudioEmbeddingDimensions() {
		return lmstudioEmbeddingDimensions;
	}

	public String openaiEmbeddingModel() {
		return openaiEmbeddingModel;
	}

	public String openaiChatModel() {
		return openaiChatModel;
	}

	public int openaiEmbeddingDimensions() {
		return openaiEmbeddingDimensions;
	}

	public String claudeChatModel() {
		return claudeChatModel;
	}

	public String claudeEmbeddingStack() {
		return claudeEmbeddingStack;
	}

	public String voyageEmbeddingModel() {
		return voyageEmbeddingModel;
	}

	public int voyageEmbeddingDimensions() {
		return voyageEmbeddingDimensions;
	}

	public int ragTopK() {
		return ragTopK;
	}

	public int ragMaxContextTokens() {
		return ragMaxContextTokens;
	}

	public int askRateLimitPerMinute() {
		return askRateLimitPerMinute;
	}

	public String openAiApiKey() {
		return envOrBlank("OPENAI_API_KEY");
	}

	public String anthropicApiKey() {
		return envOrBlank("ANTHROPIC_API_KEY");
	}

	public String voyageApiKey() {
		return envOrBlank("VOYAGE_API_KEY");
	}

	private static String envOrBlank(String env) {
		return EnvFileLoader.get(env);
	}

	private static String strProp(Properties props, String key, String env, String defaultValue) {
		String envVal = EnvFileLoader.get(env);
		if (envVal != null && !envVal.isBlank()) {
			return envVal;
		}
		return props.getProperty(key, defaultValue);
	}

	private static int intProp(Properties props, String key, String env, int defaultValue) {
		String envVal = EnvFileLoader.get(env);
		if (envVal != null && !envVal.isBlank()) {
			try {
				return Integer.parseInt(envVal);
			} catch (NumberFormatException ignored) {
			}
		}
		String val = props.getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
