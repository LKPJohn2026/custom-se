package com.cse.server.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import com.cse.index.InvertedIndex.SearchResult;
import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.stem.FileStemmer;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SearchHtmlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final ThreadSafeInvertedIndex index;

	public SearchHtmlServlet(ThreadSafeInvertedIndex index) {
		this.index = index;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String raw = req.getParameter("q");
		String query = raw == null ? "" : raw.strip();

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/html; charset=UTF-8");

		resp.getWriter().println("<!doctype html>");
		resp.getWriter().println("<html lang=\"en\"><head><meta charset=\"utf-8\" />");
		resp.getWriter().println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />");
		resp.getWriter().println("<title>custom-se results</title></head><body>");
		resp.getWriter().println("<h1>Search Results</h1>");
		resp.getWriter().println("<form action=\"/search\" method=\"get\">");
		resp.getWriter().println("<label>Query: <input type=\"text\" name=\"q\" value=\"" + escape(query)
				+ "\" /></label> <button type=\"submit\">Search</button>");
		resp.getWriter().println("</form>");

		if (query.isBlank()) {
			resp.getWriter().println("<p>No query provided.</p>");
			resp.getWriter().println("</body></html>");
			return;
		}

		Set<String> stems = FileStemmer.uniqueStems(query);
		List<SearchResult> results = index.partialIndex(stems);

		resp.getWriter().println("<ol>");
		for (SearchResult r : results) {
			String where = r.getLocation();
			resp.getWriter().println("<li>");
			resp.getWriter().println("<a href=\"" + escapeHref(where) + "\">" + escape(where) + "</a>");
			resp.getWriter().println(" (count=" + r.getMatches() + ", score=" + String.format(java.util.Locale.US, "%.8f", r.getScore())
					+ ")");
			resp.getWriter().println("</li>");
		}
		resp.getWriter().println("</ol>");
		resp.getWriter().println("</body></html>");
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String escapeHref(String href) {
		try {
			URI uri = URI.create(href);
			return escape(uri.toString());
		} catch (IllegalArgumentException e) {
			return "#";
		}
	}
}

