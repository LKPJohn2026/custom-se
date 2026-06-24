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

public class LocationBrowserServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SessionService.onPageVisit(req);
		UserSessionData session = SessionService.get(req);
		String prefix = req.getParameter("prefix");
		Set<String> all = app().index().listLocations();
		Set<String> filtered = new TreeSet<>();
		if (prefix != null && !prefix.isBlank()) {
			String p = prefix.strip().toLowerCase();
			for (String loc : all) {
				if (loc.toLowerCase().contains(p)) {
					filtered.add(loc);
				}
			}
		} else {
			filtered.addAll(all);
		}
		int page = PageSlice.parsePage(req.getParameter("page"));
		int size = PageSlice.parseSize(req.getParameter("size"));
		List<String> slice = PageSlice.slice(filtered, page, size);
		String prefixVal = prefix == null ? "" : HtmlRenderer.escape(prefix);
		String pager = PageSlice.pager("/locations?prefix=" + prefixVal, page, size, filtered.size());
		String body = """
				<h2 class="title is-4">Location Browser</h2>
				<form action="/locations" method="get" class="box">
				  <div class="field has-addons">
				    <div class="control is-expanded">
				      <input class="input" type="text" name="prefix" placeholder="Filter locations" value="%s" />
				    </div>
				    <div class="control"><button class="button is-primary" type="submit">Filter</button></div>
				  </div>
				</form>
				%s
				%s
				""".formatted(prefixVal, pager, HtmlRenderer.locationList(
				slice.stream().collect(Collectors.toCollection(TreeSet::new))));
		writeHtml(resp, HtmlRenderer.page(app(), session, "Locations", body));
	}
}
