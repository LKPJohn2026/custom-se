package com.cse.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ServerSmokeTest {
	@Test
	public void testHealthAndSearch(@TempDir Path dir) throws Exception {
		Path input = dir.resolve("docs");
		Files.createDirectories(input);
		Files.writeString(input.resolve("one.txt"), "hello world hello");

		ServerConfig config = ServerConfig.fromArgs(new String[] { "-port", "0", "-threads", "2", "-text", input.toString() });
		var index = IndexBuilder.build(config);

		AppContext ctx = new AppContext(index, 2);
		JettyServer server = new JettyServer(0, ctx);
		server.start();

		int port = server.getLocalPort();
		CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

		try {
			URI health = URI.create("http://localhost:" + port + "/api/health");
			HttpRequest hreq = HttpRequest.newBuilder(health).GET().build();
			String hbody = client.send(hreq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(hbody.contains("\"ok\"") || hbody.contains("ok"));

			URI page = URI.create("http://localhost:" + port + "/");
			HttpRequest preq = HttpRequest.newBuilder(page).GET().build();
			String pbody = client.send(preq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(pbody.toLowerCase().contains("<form"));
			assertTrue(pbody.toLowerCase().contains("name=\"q\""));
			assertTrue(pbody.contains("Uptime"));

			URI html = URI.create("http://localhost:" + port + "/search?q=hello");
			HttpRequest htmlReq = HttpRequest.newBuilder(html).GET().build();
			String htmlBody = client.send(htmlReq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(htmlBody.toLowerCase().contains("<ol>"));

			URI history = URI.create("http://localhost:" + port + "/history");
			HttpRequest histReq = HttpRequest.newBuilder(history).GET().build();
			String histBody = client.send(histReq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(histBody.contains("hello"));

			URI search = URI.create("http://localhost:" + port + "/api/search?q=hello&partial=false&limit=10");
			HttpRequest sreq = HttpRequest.newBuilder(search).GET().build();
			String sbody = client.send(sreq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(sbody.contains("\"results\""));

			URI indexPage = URI.create("http://localhost:" + port + "/index");
			String indexBody = client.send(HttpRequest.newBuilder(indexPage).GET().build(),
					HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(indexBody.contains("hello") || indexBody.contains("world"));

			URI download = URI.create("http://localhost:" + port + "/download?file=index&type=json");
			HttpResponse<Path> dl = client.send(HttpRequest.newBuilder(download).GET().build(),
					HttpResponse.BodyHandlers.ofFile(Path.of(System.getProperty("java.io.tmpdir"), "index-dl.json")));
			assertTrue(Files.size(dl.body()) > 0);
			Files.deleteIfExists(dl.body());
		} finally {
			server.stop();
		}
	}
}
