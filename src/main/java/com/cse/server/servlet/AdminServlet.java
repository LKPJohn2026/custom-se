package com.cse.server.servlet;

import java.io.IOException;

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
		String body = """
				<h2 class="title is-4">Admin</h2>
				<form action="/admin/shutdown" method="post" class="box">
				"""
				+ HtmlRenderer.csrfInput(session)
				+ """
				  <div class="field">
				    <label class="label">Password</label>
				    <div class="control"><input class="input" type="password" name="password" /></div>
				  </div>
				  <button class="button is-danger" type="submit">Graceful Shutdown</button>
				</form>
				""";
		writeHtml(resp, HtmlRenderer.page(app(), session, "Admin", body));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!requireCsrf(req, resp)) {
			return;
		}
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
