package com.cse.server.view;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cse.index.InvertedIndex.SearchResult;
import com.cse.server.AppContext;
import com.cse.server.meta.PageMetadata;
import com.cse.server.session.TimestampedEntry;
import com.cse.server.session.UserSessionData;

/**
 * Shared HTML layout, navigation, and page fragments.
 */
public final class HtmlRenderer {
	private static final String BRAND = "custom-se";
	private static final String TAGLINE = "Multi-threaded search engine";
	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private HtmlRenderer() {
	}

	public static String page(AppContext app, UserSessionData session, String title, String body) {
		boolean dark = session != null && session.isDarkMode();
		String themeClass = dark ? "theme-dark" : "theme-light";
		StringBuilder sb = new StringBuilder();
		sb.append("<!doctype html><html lang=\"en\" class=\"").append(themeClass).append("\"><head>");
		sb.append("<meta charset=\"utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />");
		sb.append("<title>").append(escape(title)).append(" — ").append(BRAND).append("</title>");
		sb.append("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bulma@1.0.2/css/bulma.min.css\" />");
		sb.append("<style>");
		sb.append(".theme-dark{background:#1a1a2e;color:#eee;min-height:100vh;}");
		sb.append(".theme-dark .box,.theme-dark .navbar,.theme-dark .footer{background:#16213e;color:#eee;}");
		sb.append(".theme-dark a{color:#7ec8e3;}");
		sb.append(".theme-light{background:#f5f5f5;color:#333;min-height:100vh;}");
		sb.append(".brand-logo{height:2rem;vertical-align:middle;margin-right:.5rem;}");
		sb.append(".server-footer{font-size:.85rem;margin-top:2rem;}");
		sb.append(".snippet{color:#888;font-size:.9rem;}");
		sb.append(".fav-btn{cursor:pointer;border:none;background:none;font-size:1.2rem;}");
		sb.append("</style></head><body>");
		sb.append(nav(session));
		sb.append("<section class=\"section\"><div class=\"container\">");
		sb.append("<div class=\"level\"><div class=\"level-left\">");
		sb.append("<img src=\"/logo.svg\" alt=\"logo\" class=\"brand-logo\" onerror=\"this.style.display='none'\" />");
		sb.append("<div><h1 class=\"title is-3\">").append(escape(BRAND)).append("</h1>");
		sb.append("<p class=\"subtitle is-6\">").append(escape(TAGLINE)).append("</p></div></div></div>");
		sb.append(body);
		if (session != null && session.lastVisit() != null) {
			sb.append("<p class=\"has-text-grey is-size-7\">Last visit: ")
					.append(formatTime(session.lastVisit())).append("</p>");
		}
		sb.append(footer(app));
		sb.append("</div></section>");
		sb.append("<script>");
		sb.append("document.querySelectorAll('.fav-form').forEach(f=>f.addEventListener('submit',async e=>{");
		sb.append("e.preventDefault();const fd=new FormData(f);const r=await fetch('/favorites/toggle',{method:'POST',body:fd});");
		sb.append("if(r.ok){const btn=f.querySelector('.fav-btn');const on=btn.textContent.trim()==='☆';");
		sb.append("btn.textContent=on?'★':'☆';alert(on?'Added to favorites':'Removed from favorites');}}));");
		sb.append("</script></body></html>");
		return sb.toString();
	}

	public static String nav(UserSessionData session) {
		boolean dark = session != null && session.isDarkMode();
		StringBuilder sb = new StringBuilder();
		sb.append("<nav class=\"navbar\" role=\"navigation\"><div class=\"navbar-menu\"><div class=\"navbar-start\">");
		sb.append(link("/", "Search"));
		sb.append(link("/history", "History"));
		sb.append(link("/visited", "Visited"));
		sb.append(link("/favorites", "Favorites"));
		sb.append(link("/stats/queries", "Popular"));
		sb.append(link("/stats/visited", "Top Visited"));
		sb.append(link("/index", "Index"));
		sb.append(link("/locations", "Locations"));
		sb.append(link("/crawl", "Crawl"));
		sb.append(link("/admin", "Admin"));
		sb.append("</div><div class=\"navbar-end\">");
		sb.append("<form action=\"/theme/toggle\" method=\"post\" class=\"navbar-item\">");
		sb.append("<button class=\"button is-small\" type=\"submit\">")
				.append(dark ? "Light mode" : "Dark mode").append("</button></form>");
		sb.append("<form action=\"/private/toggle\" method=\"post\" class=\"navbar-item\">");
		sb.append("<button class=\"button is-small\" type=\"submit\">Private: ")
				.append(session != null && session.isPrivateSearch() ? "ON" : "OFF")
				.append("</button></form>");
		sb.append("</div></div></nav>");
		return sb.toString();
	}

	private static String link(String href, String label) {
		return "<a class=\"navbar-item\" href=\"" + escape(href) + "\">" + escape(label) + "</a>";
	}

	public static String searchForm(String query, boolean partial, boolean reverse, boolean lucky) {
		String q = query == null ? "" : query;
		StringBuilder sb = new StringBuilder();
		sb.append("<form action=\"/search\" method=\"get\" class=\"box\">");
		sb.append("<div class=\"field has-addons\"><div class=\"control is-expanded\">");
		sb.append("<input class=\"input\" type=\"text\" name=\"q\" value=\"").append(escape(q))
				.append("\" placeholder=\"Enter search query\" /></div>");
		sb.append("<div class=\"control\"><button class=\"button is-primary\" type=\"submit\">Search</button></div>");
		if (lucky) {
			sb.append("<div class=\"control\"><button class=\"button is-info\" name=\"lucky\" value=\"1\" type=\"submit\">I'm Feeling Lucky</button></div>");
		}
		sb.append("</div>");
		sb.append("<div class=\"field is-grouped mt-2\">");
		sb.append(checkbox("partial", "Partial search", partial));
		sb.append(checkbox("reverse", "Reverse sort", reverse));
		sb.append("</div></form>");
		return sb.toString();
	}

	private static String checkbox(String name, String label, boolean checked) {
		return "<label class=\"checkbox mr-4\"><input type=\"checkbox\" name=\"" + name + "\" value=\"true\""
				+ (checked ? " checked" : "") + " /> " + escape(label) + "</label>";
	}

	public static String resultsList(List<SearchResult> results, Set<String> favorites, AppContext app) {
		if (results.isEmpty()) {
			return "<p class=\"notification is-warning\">No results found.</p>";
		}
		StringBuilder sb = new StringBuilder("<ol>");
		for (SearchResult r : results) {
			String where = r.getLocation();
			boolean fav = favorites != null && favorites.contains(where);
			sb.append("<li class=\"mb-3\">");
			sb.append("<form action=\"/favorites/toggle\" method=\"post\" class=\"fav-form\" style=\"display:inline\">");
			sb.append("<input type=\"hidden\" name=\"where\" value=\"").append(escape(where)).append("\" />");
			sb.append("<button class=\"fav-btn\" type=\"submit\" title=\"Toggle favorite\">")
					.append(fav ? "★" : "☆").append("</button></form> ");
			sb.append("<a href=\"").append(escapeHref(where)).append("\" onclick=\"fetch('/visit?where="
					+ encodeURIComponent(where) + "',{method:'POST'})\">")
					.append(escape(where)).append("</a>");
			sb.append(" <span class=\"tag is-light\">count=").append(r.getMatches())
					.append(", score=").append(String.format(Locale.US, "%.8f", r.getScore())).append("</span>");
			PageMetadata meta = app.metadata().getPage(where);
			if (meta != null && meta.snippet() != null && !meta.snippet().isBlank()) {
				sb.append("<div class=\"snippet\">").append(escape(meta.snippet())).append("</div>");
			}
			sb.append("</li>");
		}
		sb.append("</ol>");
		return sb.toString();
	}

	public static String timestampList(List<TimestampedEntry> entries, boolean links) {
		if (entries.isEmpty()) {
			return "<p class=\"notification is-info\">Nothing here yet.</p>";
		}
		StringBuilder sb = new StringBuilder("<ul>");
		for (TimestampedEntry e : entries) {
			sb.append("<li>");
			if (links) {
				sb.append("<a href=\"").append(escapeHref(e.value())).append("\">")
						.append(escape(e.value())).append("</a>");
			} else {
				sb.append(escape(e.value()));
			}
			sb.append(" <span class=\"tag is-light\">").append(formatTime(e.timestamp())).append("</span></li>");
		}
		sb.append("</ul>");
		return sb.toString();
	}

	public static String topList(List<java.util.Map.Entry<String, Long>> entries, boolean links) {
		if (entries.isEmpty()) {
			return "<p class=\"notification is-info\">No data yet.</p>";
		}
		StringBuilder sb = new StringBuilder("<ol>");
		for (var e : entries) {
			sb.append("<li>");
			if (links) {
				sb.append("<a href=\"").append(escapeHref(e.getKey())).append("\">")
						.append(escape(e.getKey())).append("</a>");
			} else {
				sb.append(escape(e.getKey()));
			}
			sb.append(" <span class=\"tag is-info\">").append(e.getValue()).append("</span></li>");
		}
		sb.append("</ol>");
		return sb.toString();
	}

	public static String wordList(Set<String> words) {
		if (words.isEmpty()) {
			return "<p>No words indexed.</p>";
		}
		StringBuilder sb = new StringBuilder("<ul class=\"columns is-multiline\">");
		for (String w : words) {
			sb.append("<li class=\"column is-2\"><a href=\"/index?word=")
					.append(encodeURIComponent(w)).append("\">").append(escape(w)).append("</a></li>");
		}
		sb.append("</ul>");
		return sb.toString();
	}

	public static String locationList(Set<String> locations) {
		if (locations.isEmpty()) {
			return "<p>No locations indexed.</p>";
		}
		StringBuilder sb = new StringBuilder("<ul>");
		for (String loc : locations) {
			sb.append("<li><a href=\"").append(escapeHref(loc)).append("\">")
					.append(escape(loc)).append("</a></li>");
		}
		sb.append("</ul>");
		return sb.toString();
	}

	public static String clearForm(String action) {
		return "<form action=\"" + escape(action) + "\" method=\"post\" class=\"mt-3\">"
				+ "<button class=\"button is-danger is-small\" type=\"submit\">Clear all</button></form>";
	}

	private static String footer(AppContext app) {
		var index = app.index();
		var stats = app.stats();
		return "<footer class=\"footer server-footer\"><div class=\"content has-text-centered\">"
				+ "<p>Uptime: " + escape(stats.uptime())
				+ " | Queries: " + stats.totalQueries()
				+ " | Words: " + index.getWords().size()
				+ " | Locations: " + index.getLocations().size()
				+ " | Started: " + formatTime(stats.startTime())
				+ "</p>"
				+ "<p><a href=\"/download?file=index&type=json\">Download JSON</a> | "
				+ "<a href=\"/download?file=index&type=yaml\">Download YAML</a></p>"
				+ "</div></footer>";
	}

	public static String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	public static String escapeHref(String href) {
		try {
			return escape(URI.create(href).toString());
		} catch (IllegalArgumentException e) {
			return "#";
		}
	}

	private static String encodeURIComponent(String s) {
		return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
	}

	private static String formatTime(Instant instant) {
		return instant == null ? "—" : TIME_FMT.format(instant);
	}
}
