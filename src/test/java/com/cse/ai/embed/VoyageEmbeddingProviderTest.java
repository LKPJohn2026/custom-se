package com.cse.ai.embed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VoyageEmbeddingProviderTest {
	private HttpServer server;
	private String capturedBody;
	private URI embedUri;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/v1/embeddings", exchange -> {
			capturedBody = new String(exchange.getRequestBody().readAllBytes());
			byte[] body = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]},{\"embedding\":[0.4,0.5,0.6]}]}".getBytes();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		embedUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/embeddings");
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testVoyageQueryEmbed() {
		var provider = new VoyageEmbeddingProvider(HttpClient.newHttpClient(), embedUri,
				"pa-test123456789012345678", "voyage-4", 3);
		assertEquals("voyage", provider.providerId());
		assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, provider.embedQuery("hello"), 0.0001f);
		assertTrue(capturedBody.contains("\"input_type\":\"query\""));
	}

	@Test
	public void testVoyageDocumentBatch() {
		var provider = new VoyageEmbeddingProvider(HttpClient.newHttpClient(), embedUri,
				"pa-test123456789012345678", "voyage-4", 3);
		List<float[]> vectors = provider.embedBatchDocuments(List.of("doc one", "doc two"));
		assertEquals(2, vectors.size());
		assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, vectors.get(0), 0.0001f);
		assertTrue(capturedBody.contains("\"input_type\":\"document\""));
	}
}
