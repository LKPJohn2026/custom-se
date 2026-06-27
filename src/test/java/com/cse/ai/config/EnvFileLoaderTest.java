package com.cse.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EnvFileLoaderTest {
	@TempDir
	Path tempDir;

	@AfterEach
	public void tearDown() {
		EnvFileLoader.resetForTests();
	}

	@Test
	public void testParseEnvFile() throws Exception {
		Path env = tempDir.resolve(".env");
		Files.writeString(env, """
				# comment
				OLLAMA_BASE_URL=http://localhost:11434
				OPENAI_API_KEY=sk-test123456789012345678
				""");
		var values = EnvFileLoader.parseForTests(env);
		assertEquals("http://localhost:11434", values.get("OLLAMA_BASE_URL"));
		assertEquals("sk-test123456789012345678", values.get("OPENAI_API_KEY"));
	}

	@Test
	public void testLoadRequiredMissingFile() {
		EnvFileLoader.resetForTests();
		AiConfigException ex = assertThrows(AiConfigException.class, EnvFileLoader::loadRequired);
		assertTrue(ex.getMessage().contains(".env"));
	}
}
