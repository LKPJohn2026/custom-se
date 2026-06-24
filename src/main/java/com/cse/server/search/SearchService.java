package com.cse.server.search;

import java.io.IOException;

import com.cse.index.SearchOptions;
import com.cse.search.SearchEngine;
import com.cse.server.AppContext;
import com.cse.server.session.UserSessionData;

/**
 * Server-side search adapter over {@link SearchEngine}.
 */
public final class SearchService {
	private SearchService() {
	}

	public static SearchEngine.SearchResponse search(AppContext app, UserSessionData session, String query,
			boolean partial, boolean reverse, boolean lucky) throws IOException {
		SearchOptions options = new SearchOptions(SearchOptions.DEFAULT_LIMIT, 0, reverse, lucky);
		return app.searchEngine().search(SearchEngine.parseQuery(query, partial), options, session);
	}
}
