package com.cse.server.servlet;

import java.io.IOException;
import java.net.URI;

import com.cse.concurrent.WorkQueue;
import com.cse.crawl.MetadataPageListener;
import com.cse.crawl.WebCrawler;
import com.cse.server.AppContext;
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
		if (req.getServletPath().endsWith("/status")) {
			var job = app().crawlJobs().state();
			String body = "<h2 class=\"title is-4\">Crawl Status</h2>"
					+ "<p>Status: " + HtmlRenderer.escape(job.status()) + "</p>"
					+ "<p>Crawled: " + job.crawled() + " / " + job.max() + "</p>"
					+ "<p>" + HtmlRenderer.escape(job.message()) + "</p>"
					+ "<p><a href=\"/crawl\">Back to crawl form</a></p>";
			writeHtml(resp, HtmlRenderer.page(app(), session, "Crawl Status", body));
			return;
		}
		String body = """
				<h2 class="title is-4">Crawl New Seed</h2>
				<form action="/crawl" method="post" class="box">
				"""
				+ HtmlRenderer.csrfInput(session)
				+ """
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
				<p><a href="/crawl/status">View crawl status</a></p>
				""";
		writeHtml(resp, HtmlRenderer.page(app(), session, "Crawl", body));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!requireCsrf(req, resp)) {
			return;
		}
		if (!rateLimitSearch(req, resp)) {
			return;
		}
		SessionService.onPageVisit(req);
		String seed = req.getParameter("seed");
		int max = parseInt(req.getParameter("max"), 10);
		if (seed == null || seed.isBlank()) {
			UserSessionData session = SessionService.get(req);
			writeHtml(resp, HtmlRenderer.page(app(), session, "Crawl",
					"<p class=\"notification is-danger\">Seed URI is required.</p>"));
			return;
		}
		if (app().crawlJobs().isRunning()) {
			redirect(resp, "/crawl/status");
			return;
		}
		app().crawlJobs().start(max);
		AppContext ctx = app();
		String seedUri = seed.strip();
		new Thread(() -> runCrawl(ctx, seedUri, max), "crawl-job").start();
		redirect(resp, "/crawl/status");
	}

	private static void runCrawl(AppContext ctx, String seed, int max) {
		try {
			URI uri = URI.create(seed);
			var index = ctx.index();
			WorkQueue queue = ctx.newWorkQueue();
			WebCrawler crawler = new WebCrawler(index, queue, max,
					index.listLocations(), new MetadataPageListener(ctx.metadata()));
			int crawled = crawler.crawlAdditional(uri);
			index.commit();
			queue.shutdown();
			queue.join();
			ctx.crawlJobs().finish(crawled, "Crawled " + crawled + " new page(s).");
		} catch (Exception e) {
			ctx.crawlJobs().fail("Unable to crawl the seed URI.");
		}
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
