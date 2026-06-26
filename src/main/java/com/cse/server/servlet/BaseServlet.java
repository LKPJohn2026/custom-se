package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.AppContext;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Base servlet with access to shared application context.
 */
public abstract class BaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected AppContext app() {
		Object attr = getServletContext().getAttribute(AppContext.ATTR);
		if (attr instanceof AppContext ctx) {
			return ctx;
		}
		throw new IllegalStateException("AppContext not initialized");
	}

	protected void writeHtml(HttpServletResponse resp, String html) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().print(html);
	}

	protected void redirect(HttpServletResponse resp, String path) throws IOException {
		resp.sendRedirect(path);
	}

	protected static boolean parseBool(String value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(value) || "1".equals(value) || "on".equalsIgnoreCase(value);
	}

	protected boolean requireCsrf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		var session = com.cse.server.session.SessionService.get(req);
		if (!session.validateCsrf(req.getParameter("_csrf"))) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
			return false;
		}
		return true;
	}

	protected boolean rateLimitSearch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String key = req.getRemoteAddr();
		if (!app().searchRateLimiter().tryAcquire(key)) {
			resp.sendError(429, "Rate limit exceeded");
			return false;
		}
		return true;
	}

	protected boolean rateLimitAsk(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String key = req.getRemoteAddr();
		if (!app().askRateLimiter().tryAcquire(key)) {
			resp.sendError(429, "Ask rate limit exceeded");
			return false;
		}
		return true;
	}
}
