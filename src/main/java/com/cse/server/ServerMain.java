package com.cse.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Phase 1 web entry point.
 */
public class ServerMain {
	public static void main(String[] args) throws Exception {
		int port = 8080;

		Server server = new Server(port);
		server.setHandler(createPlaceholderHandler());

		server.start();
		server.join();
	}

	private static Handler createPlaceholderHandler() {
		return new AbstractHandler() {
			@Override
			public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request,
					HttpServletResponse response) {
				try {
					response.setStatus(HttpServletResponse.SC_OK);
					response.setContentType("text/plain; charset=UTF-8");
					response.getWriter().println("custom-se server: ok");
					baseRequest.setHandled(true);
				} catch (Exception ignored) {
					// ignore
				}
			}
		};
	}
}

