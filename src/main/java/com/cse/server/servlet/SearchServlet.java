package com.cse.server.servlet;

import java.io.IOException;
import java.util.List;

import com.cse.index.SearchHit;
import com.cse.index.SearchOptions;
import com.cse.search.SearchEngine;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SearchServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String q = req.getParameter("q");
		boolean partial = parseBool(req.getParameter("partial"), false);
		int limit = parseInt(req.getParameter("limit"), 0);

		if (q == null || q.isBlank()) {
			writeJson(resp, q, partial, List.of());
			return;
		}

		SearchOptions options = new SearchOptions(
				limit > 0 ? limit : SearchOptions.DEFAULT_LIMIT, 0, false, false);
		var response = app().searchEngine().search(
				SearchEngine.parseQuery(q, partial), options, null);
		List<SearchHit> results = response.results();
		if (limit > 0 && results.size() > limit) {
			results = results.subList(0, limit);
		}

		writeJson(resp, q, partial, results);
	}

	private static int parseInt(String value, int fallback) {
		try {
			return value == null ? fallback : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static void writeJson(HttpServletResponse resp, String query, boolean partial, List<SearchHit> results)
			throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json; charset=UTF-8");

		StringBuilder out = new StringBuilder();
		out.append("{");
		out.append("\"query\":").append(toJsonString(query == null ? "" : query)).append(",");
		out.append("\"partial\":").append(partial).append(",");
		out.append("\"results\":[");

		for (int i = 0; i < results.size(); i++) {
			SearchHit r = results.get(i);
			if (i > 0) {
				out.append(",");
			}
			out.append("{");
			out.append("\"where\":").append(toJsonString(r.location())).append(",");
			out.append("\"count\":").append(r.matchCount()).append(",");
			out.append("\"score\":").append(String.format(java.util.Locale.US, "%.8f", r.score()));
			out.append("}");
		}

		out.append("]}");
		resp.getWriter().println(out.toString());
	}

	private static String toJsonString(String s) {
		StringBuilder b = new StringBuilder();
		b.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
				b.append("\\\"");
				break;
			case '\\':
				b.append("\\\\");
				break;
			case '\n':
				b.append("\\n");
				break;
			case '\r':
				b.append("\\r");
				break;
			case '\t':
				b.append("\\t");
				break;
			default:
				b.append(c);
			}
		}
		b.append('"');
		return b.toString();
	}
}
