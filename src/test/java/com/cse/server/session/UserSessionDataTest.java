package com.cse.server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserSessionDataTest {
	@Test
	public void testPrivateModeClearsTracking() {
		UserSessionData data = new UserSessionData();
		data.addSearch("hello");
		data.addVisited("http://example.com");
		data.toggleFavorite("http://example.com");
		assertFalse(data.searchHistory().isEmpty());

		data.setPrivateSearch(true);
		assertTrue(data.isPrivateSearch());
		assertTrue(data.searchHistory().isEmpty());
		assertTrue(data.visitedResults().isEmpty());
		assertTrue(data.favorites().isEmpty());

		data.addSearch("secret");
		assertTrue(data.searchHistory().isEmpty());
	}

	@Test
	public void testFavoritesToggle() {
		UserSessionData data = new UserSessionData();
		data.toggleFavorite("a");
		assertEquals(1, data.favorites().size());
		data.toggleFavorite("a");
		assertTrue(data.favorites().isEmpty());
	}
}
