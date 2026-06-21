package com.pepebot.testing

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TelegramTestReporter : TestExecutionListener {
    private val startedAt: Instant = Instant.now()
    private val passed = AtomicInteger()
    private val failed = AtomicInteger()
    private val skipped = AtomicInteger()
    private val failures = ConcurrentHashMap<String, String>()

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (!testIdentifier.isTest) {
            return
        }

        when (testExecutionResult.status) {
            TestExecutionResult.Status.SUCCESSFUL -> passed.incrementAndGet()
            TestExecutionResult.Status.FAILED -> {
                failed.incrementAndGet()
                val message = testExecutionResult.throwable
                    .map { throwable -> throwable.message ?: "Test failed" }
                    .orElse("Test failed")
                failures[testIdentifier.displayName] = message
            }
            TestExecutionResult.Status.ABORTED -> skipped.incrementAndGet()
        }
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
        if (testIdentifier.isTest) {
            skipped.incrementAndGet()
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val botToken = System.getenv("TELEGRAM_BOT_TOKEN")
        val chatId = System.getenv("TELEGRAM_CHAT_ID")
        val enabled = System.getenv().getOrDefault("TELEGRAM_ENABLED", "true")
        if (!enabled.toBoolean() || isBlank(botToken) || isBlank(chatId)) {
            return
        }

        val text = buildSummary()
        sendMessage(botToken!!, chatId!!, text)
    }

    private fun buildSummary(): String {
        val failedTests = failures.entries.sortedBy { it.key }

        val builder = StringBuilder()
        builder.append("Integration Test Run Finished\n\n")
        builder.append("Passed: ").append(passed.get()).append('\n')
        builder.append("Failed: ").append(failed.get()).append('\n')
        builder.append("Skipped: ").append(skipped.get()).append('\n')
        builder.append("Duration: ").append(Duration.between(startedAt, Instant.now()).toMillis()).append("ms\n")
        if (failedTests.isNotEmpty()) {
            builder.append("\nFailures:\n")
            for (failure in failedTests) {
                builder.append("- ")
                    .append(failure.key)
                    .append(" - ")
                    .append(truncate(failure.value, 300))
                    .append('\n')
            }
        }
        return builder.toString()
    }

    private fun sendMessage(botToken: String, chatId: String, text: String) {
        val body = "{\"chat_id\":\"${escapeJson(chatId)}\",\"text\":\"${escapeJson(text)}\"}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.telegram.org/bot$botToken/sendMessage"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()

        HttpClient.newHttpClient()
            .sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .exceptionally { ex ->
                System.err.println("Telegram test report failed: ${ex.message}")
                null
            }
    }

    private fun truncate(value: String?, maxLength: Int): String {
        if (value == null || value.length <= maxLength) {
            return value ?: ""
        }
        return value.substring(0, maxLength - 1) + "..."
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun isBlank(value: String?): Boolean {
        return value.isNullOrBlank()
    }
}
