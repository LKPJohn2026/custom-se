package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.search.SearchService;
import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SearchHtmlServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!rateLimitSearch(req, resp)) {
			return;
		}
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);

		String raw = req.getParameter("q");
		String query = raw == null ? "" : raw.strip();
		boolean partial = parseBool(req.getParameter("partial"), true);
		boolean reverse = parseBool(req.getParameter("reverse"), false);
		boolean lucky = parseBool(req.getParameter("lucky"), false);

		var response = SearchService.search(app(), session, query, partial, reverse, lucky);
		if (response.luckyRedirect() != null) {
			if (!session.isPrivateSearch()) {
				session.addVisited(response.luckyRedirect());
			}
			app().metadata().recordVisit(response.luckyRedirect());
			redirect(resp, response.luckyRedirect());
			return;
		}

		StringBuilder body = new StringBuilder();
		body.append(HtmlRenderer.searchForm(query, partial, reverse, true));
		if (!query.isBlank()) {
			body.append("<p class=\"notification is-light\">")
					.append(response.results().size()).append(" result(s) in ")
					.append(response.elapsedMs()).append(" ms</p>");
			body.append(HtmlRenderer.resultsList(response.results(), session.favorites(), app()));
		}
		writeHtml(resp, HtmlRenderer.page(app(), session, "Results", body.toString()));
	}
}
