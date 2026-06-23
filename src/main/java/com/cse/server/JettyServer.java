package com.cse.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.cse.index.ThreadSafeInvertedIndex;
import com.cse.server.servlet.HealthServlet;
import com.cse.server.servlet.SearchServlet;

public class JettyServer {
	private final Server server;

	public JettyServer(int port, ThreadSafeInvertedIndex index) {
		this.server = new Server(port);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");

		context.addServlet(new ServletHolder(new HealthServlet()), "/api/health");
		context.addServlet(new ServletHolder(new SearchServlet(index)), "/api/search");

		server.setHandler(context);
	}

	public void startAndJoin() throws Exception {
		server.start();
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
	}

	public int getLocalPort() {
		return server.getURI().getPort();
	}
}

