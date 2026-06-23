package com.cse.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

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

		ResourceHandler resources = new ResourceHandler();
		resources.setWelcomeFiles(new String[] { "index.html" });
		resources.setBaseResource(Resource.newClassPathResource("/web"));

		HandlerList handlers = new HandlerList(resources, context);

		GzipHandler gzip = new GzipHandler();
		gzip.setHandler(handlers);

		server.setHandler(gzip);
	}

	public void start() throws Exception {
		server.start();
	}

	public void join() throws Exception {
		server.join();
	}

	public void startAndJoin() throws Exception {
		start();
		join();
	}

	public void stop() throws Exception {
		server.stop();
	}

	public int getLocalPort() {
		return server.getURI().getPort();
	}
}

