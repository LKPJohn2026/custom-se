package com.cse.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import com.cse.server.servlet.AdminServlet;
import com.cse.server.servlet.AiSettingsServlet;
import com.cse.server.servlet.CrawlServlet;
import com.cse.server.servlet.DownloadServlet;
import com.cse.server.servlet.FavoritesServlet;
import com.cse.server.servlet.HealthServlet;
import com.cse.server.servlet.HistoryServlet;
import com.cse.server.servlet.IndexBrowserServlet;
import com.cse.server.servlet.LocationBrowserServlet;
import com.cse.server.servlet.PrivateServlet;
import com.cse.server.servlet.SearchHtmlServlet;
import com.cse.server.servlet.SearchPageServlet;
import com.cse.server.servlet.SearchServlet;
import com.cse.server.servlet.StatsServlet;
import com.cse.server.servlet.ThemeServlet;
import com.cse.server.servlet.VisitServlet;
import com.cse.server.servlet.VisitedServlet;

public class JettyServer {
	private final Server server;
	private final AppContext context;

	public JettyServer(int port, AppContext context) {
		this.context = context;
		this.server = new Server(port);

		ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContext.setContextPath("/");
		servletContext.setAttribute(AppContext.ATTR, context);
		context.setShutdownHook(() -> {
			try {
				context.index().commit();
				context.index().close();
				server.stop();
			} catch (Exception e) {
				System.err.println("Unable to stop server.");
			}
		});

		servletContext.addServlet(new ServletHolder(new SearchPageServlet()), "/");
		servletContext.addServlet(new ServletHolder(new SearchHtmlServlet()), "/search");
		servletContext.addServlet(new ServletHolder(new HealthServlet()), "/api/health");
		servletContext.addServlet(new ServletHolder(new SearchServlet()), "/api/search");

		servletContext.addServlet(new ServletHolder(new HistoryServlet()), "/history");
		servletContext.addServlet(new ServletHolder(new HistoryServlet()), "/history/clear");
		servletContext.addServlet(new ServletHolder(new VisitedServlet()), "/visited");
		servletContext.addServlet(new ServletHolder(new VisitedServlet()), "/visited/clear");
		servletContext.addServlet(new ServletHolder(new FavoritesServlet()), "/favorites");
		servletContext.addServlet(new ServletHolder(new FavoritesServlet()), "/favorites/clear");
		servletContext.addServlet(new ServletHolder(new FavoritesServlet()), "/favorites/toggle");
		servletContext.addServlet(new ServletHolder(new PrivateServlet()), "/private/toggle");
		servletContext.addServlet(new ServletHolder(new ThemeServlet()), "/theme/toggle");
		servletContext.addServlet(new ServletHolder(new VisitServlet()), "/visit");
		servletContext.addServlet(new ServletHolder(new StatsServlet()), "/stats/queries");
		servletContext.addServlet(new ServletHolder(new StatsServlet()), "/stats/visited");
		servletContext.addServlet(new ServletHolder(new CrawlServlet()), "/crawl");
		servletContext.addServlet(new ServletHolder(new CrawlServlet()), "/crawl/status");
		servletContext.addServlet(new ServletHolder(new IndexBrowserServlet()), "/index");
		servletContext.addServlet(new ServletHolder(new LocationBrowserServlet()), "/locations");
		servletContext.addServlet(new ServletHolder(new DownloadServlet()), "/download");
		servletContext.addServlet(new ServletHolder(new AiSettingsServlet()), "/settings/ai");
		servletContext.addServlet(new ServletHolder(new AiSettingsServlet()), "/settings/ai/test");
		servletContext.addServlet(new ServletHolder(new AdminServlet()), "/admin");
		servletContext.addServlet(new ServletHolder(new AdminServlet()), "/admin/shutdown");

		ResourceHandler staticHandler = new ResourceHandler();
		staticHandler.setBaseResource(Resource.newClassPathResource("/web"));
		staticHandler.setWelcomeFiles(new String[0]);

		HandlerList handlers = new HandlerList(servletContext, staticHandler);

		GzipHandler gzip = new GzipHandler();
		gzip.setHandler(handlers);

		server.setHandler(gzip);
	}

	public AppContext context() {
		return context;
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
