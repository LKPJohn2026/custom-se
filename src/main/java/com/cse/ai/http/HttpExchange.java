package com.cse.ai.http;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP client factory and retry helper for AI provider calls.
 */
public final class HttpExchange {
	private HttpExchange() {
	}

	public static HttpClient newClient(AiHttpConfig config) {
		return HttpClient.newBuilder().connectTimeout(config.connectTimeout()).build();
	}

	public static <T> HttpResponse<T> send(HttpClient http, HttpRequest request,
			HttpResponse.BodyHandler<T> handler, AiHttpConfig config) throws IOException, InterruptedException {
		int maxAttempts = Math.max(1, config.maxRetries() + 1);
		IOException lastIo = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				HttpResponse<T> response = http.send(request, handler);
				if (attempt < maxAttempts && retryableStatus(response.statusCode())) {
					sleepBackoff(attempt);
					continue;
				}
				return response;
			} catch (IOException e) {
				lastIo = e;
				if (attempt >= maxAttempts) {
					throw e;
				}
				sleepBackoff(attempt);
			}
		}
		if (lastIo != null) {
			throw lastIo;
		}
		throw new IOException("HTTP request failed");
	}

	private static boolean retryableStatus(int status) {
		return status == 429 || status == 502 || status == 503 || status == 504;
	}

	private static void sleepBackoff(int attempt) {
		try {
			Thread.sleep(250L * attempt);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
