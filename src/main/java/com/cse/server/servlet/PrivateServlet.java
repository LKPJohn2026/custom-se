package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.session.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PrivateServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		var session = SessionService.get(req);
		session.setPrivateSearch(!session.isPrivateSearch());
		String referer = req.getHeader("Referer");
		redirect(resp, referer != null ? referer : "/");
	}
}
