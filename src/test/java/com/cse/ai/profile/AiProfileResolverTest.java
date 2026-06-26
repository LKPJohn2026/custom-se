package com.cse.ai.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AiProfileResolverTest {
	@Test
	public void testResolveDefaultStack() {
		var settings = AiSettings.load();
		var resolver = new AiProfileResolver(settings);
		AiProfile profile = resolver.resolve(AiPreferences.defaults(""));
		assertEquals(settings.defaultStack(), profile.id());
		assertTrue(resolver.availableProfiles().size() >= 2);
	}

	@Test
	public void testResolveSessionOverride() {
		var resolver = new AiProfileResolver(AiSettings.load());
		AiProfile profile = resolver.resolve(AiPreferences.defaults("lmstudio"));
		assertEquals("lmstudio", profile.id());
	}
}
