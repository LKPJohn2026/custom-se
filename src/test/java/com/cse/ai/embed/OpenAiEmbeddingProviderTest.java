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

public class OpenAiEmbeddingProviderTest {
	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/v1/embeddings", exchange -> {
			byte[] body = "{\"data\":[{\"embedding\":[0.4,0.5,0.6]}]}".getBytes();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1/embeddings";
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testOpenAiEmbed() {
		var provider = new OpenAiEmbeddingProvider(HttpClient.newHttpClient(), baseUrl, "test-key",
				"text-embedding-3-small", 3);
		assertEquals("openai", provider.providerId());
		assertArrayEquals(new float[] { 0.4f, 0.5f, 0.6f }, provider.embed("hello"), 0.0001f);
	}

	@Test
	public void testCompatibleEmbed() {
		String root = "http://localhost:" + server.getAddress().getPort() + "/v1";
		var provider = new OpenAiCompatibleEmbeddingProvider(root, "local-embed", 3);
		assertEquals("openai-compatible", provider.providerId());
		assertArrayEquals(new float[] { 0.4f, 0.5f, 0.6f }, provider.embed("hello"), 0.0001f);
	}
}
