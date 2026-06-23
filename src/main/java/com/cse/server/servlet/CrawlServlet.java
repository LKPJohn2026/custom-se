package com.cse.server.servlet;

import java.io.IOException;
import java.net.URI;

import com.cse.concurrent.WorkQueue;
import com.cse.crawl.WebCrawler;
import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CrawlServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String body = """
				<h2 class="title is-4">Crawl New Seed</h2>
				<form action="/crawl" method="post" class="box">
				  <div class="field">
				    <label class="label">Seed URI</label>
				    <div class="control"><input class="input" type="text" name="seed" placeholder="https://example.com" /></div>
				  </div>
				  <div class="field">
				    <label class="label">Max new pages (optional)</label>
				    <div class="control"><input class="input" type="number" name="max" min="1" value="10" /></div>
				  </div>
				  <button class="button is-primary" type="submit">Start Crawl</button>
				</form>
				<p class="help">Already-indexed URLs are skipped and do not count toward the max.</p>
				""";
		writeHtml(resp, HtmlRenderer.page(app(), session, "Crawl", body));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String seed = req.getParameter("seed");
		int max = parseInt(req.getParameter("max"), 10);
		String message;
		if (seed == null || seed.isBlank()) {
			message = "<p class=\"notification is-danger\">Seed URI is required.</p>";
		} else {
			try {
				URI uri = URI.create(seed.strip());
				WorkQueue queue = app().newWorkQueue();
				var index = app().index();
				WebCrawler crawler = new WebCrawler(index, queue, max,
						index.getLocations(), app().metadata());
				int crawled = crawler.crawlAdditional(uri);
				queue.shutdown();
				queue.join();
				message = "<p class=\"notification is-success\">Crawled " + crawled
						+ " new page(s) from seed.</p>";
			} catch (Exception e) {
				message = "<p class=\"notification is-danger\">Unable to crawl the seed URI.</p>";
			}
		}
		String body = message + """
				<p><a href="/crawl">Back to crawl form</a></p>
				""";
		writeHtml(resp, HtmlRenderer.page(app(), session, "Crawl Result", body));
	}

	private static int parseInt(String value, int fallback) {
		try {
			int n = value == null ? fallback : Integer.parseInt(value);
			return n > 0 ? n : fallback;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
