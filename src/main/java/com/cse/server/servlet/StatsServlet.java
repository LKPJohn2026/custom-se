package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatsServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String path = req.getServletPath();
		String body;
		String title;
		if (path.contains("visited")) {
			title = "Top Visited";
			body = "<h2 class=\"title is-4\">Most Visited Results (Top 5)</h2>"
					+ HtmlRenderer.topList(app().metadata().topVisits(5), true);
		} else {
			title = "Popular Queries";
			body = "<h2 class=\"title is-4\">Popular Queries (Top 5)</h2>"
					+ HtmlRenderer.topList(app().metadata().topQueries(5), false);
		}
		writeHtml(resp, HtmlRenderer.page(app(), session, title, body));
	}
}
