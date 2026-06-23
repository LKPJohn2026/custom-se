package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class VisitedServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String body = "<h2 class=\"title is-4\">Visited Results</h2>"
				+ HtmlRenderer.timestampList(session.visitedResults(), true)
				+ HtmlRenderer.clearForm("/visited/clear");
		writeHtml(resp, HtmlRenderer.page(app(), session, "Visited", body));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.get(req).clearVisited();
		redirect(resp, "/visited");
	}
}
