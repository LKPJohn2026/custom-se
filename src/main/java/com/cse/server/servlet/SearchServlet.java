package com.cse.server.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.cse.index.InvertedIndex.SearchResult;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.stem.FileStemmer;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final ThreadSafeInvertedIndex index;

	public SearchServlet(ThreadSafeInvertedIndex index) {
		this.index = index;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String q = req.getParameter("q");
		boolean partial = Boolean.parseBoolean(req.getParameter("partial"));
		int limit = parseInt(req.getParameter("limit"), 0);

		if (q == null || q.isBlank()) {
			writeJson(resp, q, partial, Collections.emptyList());
			return;
		}

		Set<String> stems = FileStemmer.uniqueStems(q);
		List<SearchResult> results = partial ? index.partialIndex(stems) : index.exactIndex(stems);

		if (limit > 0 && results.size() > limit) {
			results = new ArrayList<>(results.subList(0, limit));
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

	private static void writeJson(HttpServletResponse resp, String query, boolean partial, List<SearchResult> results)
			throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json; charset=UTF-8");

		StringBuilder out = new StringBuilder();
		out.append("{");
		out.append("\"query\":").append(toJsonString(query == null ? "" : query)).append(",");
		out.append("\"partial\":").append(partial).append(",");
		out.append("\"results\":[");

		for (int i = 0; i < results.size(); i++) {
			SearchResult r = results.get(i);
			if (i > 0) {
				out.append(",");
			}
			out.append("{");
			out.append("\"where\":").append(toJsonString(r.getLocation())).append(",");
			out.append("\"count\":").append(r.getMatches()).append(",");
			out.append("\"score\":").append(String.format(java.util.Locale.US, "%.8f", r.getScore()));
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

