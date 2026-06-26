package com.cse.ai.embed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class OllamaEmbeddingProviderTest {
	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/embeddings", exchange -> {
			byte[] body = "{\"embedding\":[0.1,0.2,0.3]}".getBytes();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		baseUrl = "http://localhost:" + server.getAddress().getPort();
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testEmbed() {
		var provider = new OllamaEmbeddingProvider(HttpClient.newHttpClient(), baseUrl, "nomic-embed-text", 3);
		assertEquals("ollama", provider.providerId());
		assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, provider.embed("hello"), 0.0001f);
	}
}
