package com.cse.ai.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AiSettingsTest {
	@Test
	public void testDefaults() {
		AiSettings settings = AiSettings.load();
		assertEquals("ollama", settings.defaultStack());
		assertEquals(8, settings.ragTopK());
	}
}
