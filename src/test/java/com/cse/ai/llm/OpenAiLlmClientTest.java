package com.cse.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class OpenAiLlmClientTest {
	private HttpServer server;
	private String chatUrl;

	@BeforeEach
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/v1/chat/completions", exchange -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] response;
			if (body.contains("\"stream\":true")) {
				response = "data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}\n\ndata: [DONE]\n".getBytes();
				exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
			} else {
				response = "{\"choices\":[{\"message\":{\"content\":\"Hello\"}}]}".getBytes();
				exchange.getResponseHeaders().add("Content-Type", "application/json");
			}
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(response);
			}
		});
		server.start();
		chatUrl = "http://localhost:" + server.getAddress().getPort() + "/v1/chat/completions";
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void testOpenAiComplete() {
		var client = new OpenAiLlmClient(HttpClient.newHttpClient(), chatUrl, "key", "gpt-4o-mini");
		assertEquals("Hello", client.completeChat(ChatRequest.of("sys", "q")));
	}

	@Test
	public void testCompatibleStream() {
		String root = "http://localhost:" + server.getAddress().getPort() + "/v1";
		var client = new OpenAiCompatibleLlmClient(root, "local");
		String text = client.streamChat(ChatRequest.of("sys", "q")).collect(Collectors.joining());
		assertEquals("Hi", text);
		assertEquals("openai-compatible", client.providerId());
	}
}
