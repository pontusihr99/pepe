package com.pepebot.testing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class TelegramTestReporter implements TestExecutionListener {

	private final Instant startedAt = Instant.now();
	private final AtomicInteger passed = new AtomicInteger();
	private final AtomicInteger failed = new AtomicInteger();
	private final AtomicInteger skipped = new AtomicInteger();
	private final Map<String, String> failures = new ConcurrentHashMap<>();

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (!testIdentifier.isTest()) {
			return;
		}

		switch (testExecutionResult.getStatus()) {
			case SUCCESSFUL -> passed.incrementAndGet();
			case FAILED -> {
				failed.incrementAndGet();
				failures.put(testIdentifier.getDisplayName(), testExecutionResult.getThrowable()
						.map(Throwable::getMessage)
						.orElse("Test failed"));
			}
			case ABORTED -> skipped.incrementAndGet();
			default -> {
			}
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isTest()) {
			skipped.incrementAndGet();
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
		String chatId = System.getenv("TELEGRAM_CHAT_ID");
		String enabled = System.getenv().getOrDefault("TELEGRAM_ENABLED", "true");
		if (!Boolean.parseBoolean(enabled) || isBlank(botToken) || isBlank(chatId)) {
			return;
		}

		String text = buildSummary();
		sendMessage(botToken, chatId, text);
	}

	private String buildSummary() {
		List<Map.Entry<String, String>> failedTests = new ArrayList<>(failures.entrySet());
		failedTests.sort(Comparator.comparing(Map.Entry::getKey));

		StringBuilder builder = new StringBuilder();
		builder.append("🧪 Integration Test Run Finished\n\n");
		builder.append("✅ Passed: ").append(passed.get()).append('\n');
		builder.append("❌ Failed: ").append(failed.get()).append('\n');
		builder.append("⏭️ Skipped: ").append(skipped.get()).append('\n');
		builder.append("⏱️ Duration: ").append(Duration.between(startedAt, Instant.now()).toMillis()).append("ms\n");
		if (!failedTests.isEmpty()) {
			builder.append("\nFailures:\n");
			for (Map.Entry<String, String> failure : failedTests) {
				builder.append("• ").append(failure.getKey()).append(" — ").append(truncate(failure.getValue(), 300)).append('\n');
			}
		}
		return builder.toString();
	}

	private void sendMessage(String botToken, String chatId, String text) {
		String body = "{\"chat_id\":\"" + escapeJson(chatId) + "\",\"text\":\"" + escapeJson(text) + "\"}";
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.exceptionally(ex -> {
					System.err.println("Telegram test report failed: " + ex.getMessage());
					return null;
				});
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value == null ? "" : value;
		}
		return value.substring(0, maxLength - 1) + "…";
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
