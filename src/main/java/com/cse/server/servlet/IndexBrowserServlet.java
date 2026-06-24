package com.cse.server.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.cse.server.session.SessionService;
import com.cse.server.session.UserSessionData;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class IndexBrowserServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String word = req.getParameter("word");
		String body;
		if (word != null && !word.isBlank()) {
			Set<String> locations = app().index().locationsForTerm(word.strip());
			body = "<h2 class=\"title is-4\">Locations for \"" + HtmlRenderer.escape(word.strip()) + "\"</h2>"
					+ HtmlRenderer.locationList(new TreeSet<>(locations))
					+ "<p><a href=\"/index\">Back to all words</a></p>";
		} else {
			int page = PageSlice.parsePage(req.getParameter("page"));
			int size = PageSlice.parseSize(req.getParameter("size"));
			Set<String> all = app().index().listTerms();
			List<String> slice = PageSlice.slice(all, page, size);
			body = "<h2 class=\"title is-4\">Index Browser</h2>"
					+ PageSlice.pager("/index", page, size, all.size())
					+ HtmlRenderer.wordList(slice.stream().collect(Collectors.toCollection(TreeSet::new)));
		}
		writeHtml(resp, HtmlRenderer.page(app(), session, "Index", body));
	}
}
