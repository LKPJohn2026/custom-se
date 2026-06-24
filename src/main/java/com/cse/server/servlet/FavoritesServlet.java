package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FavoritesServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		var favs = session.favorites();
		StringBuilder body = new StringBuilder("<h2 class=\"title is-4\">Favorites</h2>");
		if (favs.isEmpty()) {
			body.append("<p class=\"notification is-info\">No favorites yet. Use ☆ on search results.</p>");
		} else {
			body.append("<ul>");
			for (String f : favs) {
				body.append("<li><a href=\"").append(HtmlRenderer.escapeHref(f)).append("\">")
						.append(HtmlRenderer.escape(f)).append("</a> ");
				body.append("<form action=\"/favorites/toggle\" method=\"post\" style=\"display:inline\">");
				body.append("<input type=\"hidden\" name=\"redirect\" value=\"true\" />");
				body.append("<input type=\"hidden\" name=\"where\" value=\"").append(HtmlRenderer.escape(f))
						.append("\" /><button class=\"button is-small\" type=\"submit\">Remove</button></form></li>");
			}
			body.append("</ul>");
		}
		body.append(HtmlRenderer.clearForm(session, "/favorites/clear"));
		writeHtml(resp, HtmlRenderer.page(app(), session, "Favorites", body.toString()));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getServletPath();
		UserSessionData session = SessionService.get(req);
		if (path.endsWith("/clear")) {
			if (!requireCsrf(req, resp)) {
				return;
			}
			session.clearFavorites();
			redirect(resp, "/favorites");
		} else if (path.endsWith("/toggle")) {
			String where = req.getParameter("where");
			session.toggleFavorite(where);
			if (parseBool(req.getParameter("redirect"), false)) {
				String referer = req.getHeader("Referer");
				redirect(resp, referer != null ? referer : "/favorites");
			} else {
				resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}
		}
	}
}
