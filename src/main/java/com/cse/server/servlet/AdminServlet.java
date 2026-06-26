package com.cse.server.servlet;

import java.io.IOException;

import com.cse.ai.embed.EmbeddingIndexJob;
import com.cse.ai.profile.AiPreferences;
import com.cse.ai.profile.AiProfile;
import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		var embedState = app().embedJobs().state();
		StringBuilder body = new StringBuilder();
		body.append("<h2 class=\"title is-4\">Admin</h2>");
		body.append("<p class=\"mb-4\">Re-embed status: <strong>")
				.append(HtmlRenderer.escape(embedState.status())).append("</strong>");
		if (embedState.total() > 0) {
			body.append(" (").append(embedState.processed()).append("/").append(embedState.total()).append(")");
		}
		if (!embedState.message().isBlank()) {
			body.append(" — ").append(HtmlRenderer.escape(embedState.message()));
		}
		body.append("</p>");
		body.append("<form action=\"/admin/re-embed\" method=\"post\" class=\"box\">");
		body.append(HtmlRenderer.csrfInput(session));
		body.append("<div class=\"field\"><label class=\"label\">Password</label>");
		body.append("<div class=\"control\"><input class=\"input\" type=\"password\" name=\"password\" /></div></div>");
		body.append("<button class=\"button is-warning\" type=\"submit\"");
		if (app().embedJobs().isRunning()) {
			body.append(" disabled");
		}
		body.append(">Re-embed all chunks</button></form>");
		body.append("<form action=\"/admin/shutdown\" method=\"post\" class=\"box\">");
		body.append(HtmlRenderer.csrfInput(session));
		body.append("""
				  <div class="field">
				    <label class="label">Password</label>
				    <div class="control"><input class="input" type="password" name="password" /></div>
				  </div>
				  <button class="button is-danger" type="submit">Graceful Shutdown</button>
				</form>
				""");
		writeHtml(resp, HtmlRenderer.page(app(), session, "Admin", body.toString()));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!requireCsrf(req, resp)) {
			return;
		}
		String path = req.getServletPath();
		if (path.endsWith("/re-embed")) {
			handleReembed(req, resp);
			return;
		}
		handleShutdown(req, resp);
	}

	private void handleReembed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		UserSessionData session = SessionService.get(req);
		String password = req.getParameter("password");
		if (!app().settings().adminPassword().equals(password)) {
			writeHtml(resp, HtmlRenderer.page(app(), session, "Admin",
					"<p class=\"notification is-danger\">Invalid password.</p><p><a href=\"/admin\">Back</a></p>"));
			return;
		}
		if (app().embedJobs().isRunning()) {
			writeHtml(resp, HtmlRenderer.page(app(), session, "Admin",
					"<p class=\"notification is-warning\">Re-embed already running.</p><p><a href=\"/admin\">Back</a></p>"));
			return;
		}
		AiProfile profile = app().aiProfileResolver().resolve(AiPreferences.defaults(
				app().aiSettings().defaultStack()));
		Thread worker = new Thread(() -> EmbeddingIndexJob.run(
				app().index(), profile.embeddings(), app().embedJobs()), "re-embed");
		worker.setDaemon(true);
		worker.start();
		writeHtml(resp, HtmlRenderer.page(app(), session, "Admin",
				"<p class=\"notification is-info\">Re-embed job started.</p><p><a href=\"/admin\">Back</a></p>"));
	}

	private void handleShutdown(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String password = req.getParameter("password");
		if (!app().settings().adminPassword().equals(password)) {
			UserSessionData session = SessionService.get(req);
			String body = "<p class=\"notification is-danger\">Invalid password.</p>"
					+ "<p><a href=\"/admin\">Try again</a></p>";
			writeHtml(resp, HtmlRenderer.page(app(), session, "Admin", body));
			return;
		}
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().println("<p>Server shutting down…</p>");
		resp.getWriter().flush();
		new Thread(() -> {
			try {
				Thread.sleep(500);
				app().shutdown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "shutdown").start();
	}
}
