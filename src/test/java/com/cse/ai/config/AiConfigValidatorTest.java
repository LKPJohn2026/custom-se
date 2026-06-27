package com.cse.ai.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.cse.ai.profile.AiSettings;

public class AiConfigValidatorTest {
	@AfterEach
	public void tearDown() {
		EnvFileLoader.resetForTests();
	}

	@Test
	public void testOllamaRequiresUrl() {
		EnvFileLoader.setCachedForTests(java.util.Map.of("OLLAMA_BASE_URL", "http://localhost:11434"));
		AiSettings settings = AiSettings.load();
		assertDoesNotThrow(() -> AiConfigValidator.validate(settings, "ollama"));
	}

	@Test
	public void testOpenAiKeyFormat() {
		EnvFileLoader.setCachedForTests(java.util.Map.of("OPENAI_API_KEY", "bad-key"));
		AiSettings settings = AiSettings.load();
		AiConfigException ex = assertThrows(AiConfigException.class,
				() -> AiConfigValidator.validate(settings, "openai"));
		assertTrue(ex.getMessage().contains("OPENAI_API_KEY"));
	}

	@Test
	public void testClaudeRequiresVoyageKey() {
		EnvFileLoader.setCachedForTests(java.util.Map.of(
				"ANTHROPIC_API_KEY", "sk-ant-test123456789012345678",
				"VOYAGE_API_KEY", "pa-test123456789012345678"));
		AiSettings settings = AiSettings.load();
		assertDoesNotThrow(() -> AiConfigValidator.validate(settings, "claude"));
	}
}
