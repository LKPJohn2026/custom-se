package com.cse.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ServerSettingsTest {
	@Test
	public void testDefaults() {
		ServerSettings settings = ServerSettings.load();
		assertEquals(8080, settings.port());
		assertEquals(5, settings.threads());
		assertTrue(settings.indexDirectory().contains("index"));
	}
}
