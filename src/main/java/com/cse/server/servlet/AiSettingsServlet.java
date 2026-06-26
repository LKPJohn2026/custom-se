package com.cse.server.servlet;

import java.io.IOException;

import com.cse.ai.profile.AiPreferences;
import com.cse.ai.profile.AiProfile;
import com.cse.server.session.SessionService;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AI stack selection and connection tests.
 */
public class AiSettingsServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		var session = SessionService.get(req);
		SessionService.onPageVisit(req);
		StringBuilder body = new StringBuilder();
		body.append("<h2 class=\"title is-4\">AI Settings</h2>");
		body.append("<form method=\"post\" action=\"/settings/ai\" class=\"box\">");
		body.append("<input type=\"hidden\" name=\"_csrf\" value=\"").append(session.csrfToken()).append("\" />");
		body.append("<div class=\"field\"><label class=\"label\">Stack</label><div class=\"control\">");
		String current = session.aiPreferences().effectiveStackId(app().aiSettings().defaultStack());
		for (var descriptor : app().aiProfileResolver().availableProfiles()) {
			body.append("<label class=\"radio mr-4\">");
			body.append("<input type=\"radio\" name=\"stackId\" value=\"").append(descriptor.id()).append("\"");
			if (descriptor.id().equals(current)) {
				body.append(" checked");
			}
			if (!descriptor.available()) {
				body.append(" disabled");
			}
			body.append("> ").append(HtmlRenderer.escape(descriptor.displayName()));
			if (!descriptor.note().isBlank()) {
				body.append(" <span class=\"has-text-grey\">(").append(HtmlRenderer.escape(descriptor.note()))
						.append(")</span>");
			}
			body.append("</label><br/>");
		}
		body.append("</div></div>");
		body.append("<div class=\"field\"><button class=\"button is-primary\" type=\"submit\">Save</button></div>");
		body.append("</form>");

		body.append("<form method=\"post\" action=\"/settings/ai/test\" class=\"box\">");
		body.append("<input type=\"hidden\" name=\"_csrf\" value=\"").append(session.csrfToken()).append("\" />");
		body.append("<button class=\"button\" type=\"submit\">Test connection</button>");
		body.append("</form>");

		writeHtml(resp, HtmlRenderer.page(app(), session, "AI Settings", body.toString()));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!requireCsrf(req, resp)) {
			return;
		}
		String path = req.getServletPath();
		var session = SessionService.get(req);
		if (path.endsWith("/test")) {
			AiProfile profile = app().aiProfileResolver().resolve(session.aiPreferences());
			boolean embedOk = app().aiProfileResolver().testEmbeddings(profile);
			boolean chatOk = app().aiProfileResolver().testChat(profile);
			String msg = embedOk && chatOk ? "Connection OK" : "Connection failed";
			writeHtml(resp, HtmlRenderer.page(app(), session, "AI Settings",
					"<div class=\"notification\">" + HtmlRenderer.escape(msg) + "</div><p><a href=\"/settings/ai\">Back</a></p>"));
			return;
		}
		String stackId = req.getParameter("stackId");
		session.setAiPreferences(AiPreferences.defaults(stackId == null ? "" : stackId));
		redirect(resp, "/settings/ai");
	}
}
