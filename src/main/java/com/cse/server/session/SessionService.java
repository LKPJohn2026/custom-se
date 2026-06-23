package com.cse.server.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Accesses per-user session data from HTTP sessions.
 */
public final class SessionService {
	private static final String KEY = "userData";

	private SessionService() {
	}

	public static UserSessionData get(HttpServletRequest req) {
		HttpSession session = req.getSession(true);
		Object attr = session.getAttribute(KEY);
		if (attr instanceof UserSessionData data) {
			return data;
		}
		UserSessionData data = new UserSessionData();
		session.setAttribute(KEY, data);
		return data;
	}

	public static void onPageVisit(HttpServletRequest req) {
		UserSessionData data = get(req);
		data.touchVisit();
	}
}
