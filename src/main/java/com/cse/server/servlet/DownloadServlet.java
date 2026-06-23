package com.cse.server.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.cse.server.view.YamlWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DownloadServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String type = req.getParameter("type");
		String file = req.getParameter("file");
		if (!"index".equals(file)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file");
			return;
		}
		if ("json".equalsIgnoreCase(type)) {
			downloadJson(resp);
		} else if ("yaml".equalsIgnoreCase(type)) {
			downloadYaml(resp);
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported type");
		}
	}

	private void downloadJson(HttpServletResponse resp) throws IOException {
		Path temp = Files.createTempFile("index-", ".json");
		try {
			app().index().indexJson(temp);
			resp.setContentType("application/json; charset=UTF-8");
			resp.setHeader("Content-Disposition", "attachment; filename=\"index.json\"");
			Files.copy(temp, resp.getOutputStream());
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private void downloadYaml(HttpServletResponse resp) throws IOException {
		Map<String, TreeMap<String, TreeSet<Integer>>> snapshot = snapshotIndex();
		resp.setContentType("text/yaml; charset=UTF-8");
		resp.setHeader("Content-Disposition", "attachment; filename=\"index.yaml\"");
		YamlWriter.writeIndex(snapshot, resp.getWriter());
	}

	private Map<String, TreeMap<String, TreeSet<Integer>>> snapshotIndex() {
		var index = app().index();
		Map<String, TreeMap<String, TreeSet<Integer>>> out = new TreeMap<>();
		for (String word : index.getWords()) {
			TreeMap<String, TreeSet<Integer>> locs = new TreeMap<>();
			for (String loc : index.getLocations(word)) {
				// positions not exposed via public API; export locations only
				locs.put(loc, new TreeSet<>());
			}
			out.put(word, locs);
		}
		return out;
	}
}
