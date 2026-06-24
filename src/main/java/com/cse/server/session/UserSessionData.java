package com.cse.server.session;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-user session state stored in the HTTP session.
 */
public class UserSessionData implements Serializable {
	private static final long serialVersionUID = 1L;

	private boolean privateSearch;
	private boolean darkMode;
	private Instant lastVisit;
	private final String csrfToken = UUID.randomUUID().toString();
	private final List<TimestampedEntry> searchHistory = new ArrayList<>();
	private final List<TimestampedEntry> visitedResults = new ArrayList<>();
	private final Set<String> favorites = new LinkedHashSet<>();

	public boolean isPrivateSearch() {
		return privateSearch;
	}

	public void setPrivateSearch(boolean privateSearch) {
		this.privateSearch = privateSearch;
		if (privateSearch) {
			clearTracking();
		}
	}

	public boolean isDarkMode() {
		return darkMode;
	}

	public void setDarkMode(boolean darkMode) {
		this.darkMode = darkMode;
	}

	public Instant lastVisit() {
		return lastVisit;
	}

	public void touchVisit() {
		lastVisit = Instant.now();
	}

	public String csrfToken() {
		return csrfToken;
	}

	public boolean validateCsrf(String token) {
		return token != null && csrfToken.equals(token);
	}

	public List<TimestampedEntry> searchHistory() {
		return List.copyOf(searchHistory);
	}

	public void addSearch(String query) {
		if (!privateSearch && query != null && !query.isBlank()) {
			searchHistory.add(0, new TimestampedEntry(query.strip()));
		}
	}

	public void clearHistory() {
		searchHistory.clear();
	}

	public List<TimestampedEntry> visitedResults() {
		return List.copyOf(visitedResults);
	}

	public void addVisited(String location) {
		if (!privateSearch && location != null && !location.isBlank()) {
			visitedResults.add(0, new TimestampedEntry(location));
		}
	}

	public void clearVisited() {
		visitedResults.clear();
	}

	public Set<String> favorites() {
		return Set.copyOf(favorites);
	}

	public void toggleFavorite(String location) {
		if (location == null || location.isBlank()) {
			return;
		}
		if (favorites.contains(location)) {
			favorites.remove(location);
		} else {
			favorites.add(location);
		}
	}

	public void clearFavorites() {
		favorites.clear();
	}

	private void clearTracking() {
		searchHistory.clear();
		visitedResults.clear();
		favorites.clear();
	}
}
