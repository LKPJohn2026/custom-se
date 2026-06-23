package com.cse.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

		ServerConfig config = ServerConfig.fromArgs(new String[] { "-port", "0", "-threads", "2", "-text",
				input.toString() });
		var index = IndexBuilder.build(config);

		JettyServer server = new JettyServer(0, index);
		server.start();

		int port = server.getLocalPort();
		HttpClient client = HttpClient.newHttpClient();

		try {
			URI health = URI.create("http://localhost:" + port + "/api/health");
			HttpRequest hreq = HttpRequest.newBuilder(health).GET().build();
			String hbody = client.send(hreq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(hbody.contains("\"ok\"") || hbody.contains("ok"));

			URI search = URI.create("http://localhost:" + port + "/api/search?q=hello&partial=false&limit=10");
			HttpRequest sreq = HttpRequest.newBuilder(search).GET().build();
			String sbody = client.send(sreq, HttpResponse.BodyHandlers.ofString()).body();
			assertTrue(sbody.contains("\"results\""));
		} finally {
			server.stop();
		}
	}
}

