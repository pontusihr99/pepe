package com.pepebot.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.pepebot.config.KrakenProperties
import com.pepebot.dto.AssetBalance
import com.pepebot.dto.BalanceResponse
import com.pepebot.dto.OrderType
import com.pepebot.dto.PriceResponse
import com.pepebot.dto.Side
import com.pepebot.dto.TradeResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class KrakenClient(
    private val krakenWebClient: WebClient,
    private val properties: KrakenProperties,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val PUBLIC_TICKER_PATH = "/0/public/Ticker"
        private const val PRIVATE_BALANCE_PATH = "/0/private/Balance"
        private const val PRIVATE_ADD_ORDER_PATH = "/0/private/AddOrder"
    }

    private val nonceSequence = AtomicLong(System.currentTimeMillis())

    fun getCurrentPrice(pair: String): Mono<PriceResponse> {
        return krakenWebClient.get()
            .uri { uriBuilder -> uriBuilder.path(PUBLIC_TICKER_PATH).queryParam("pair", pair).build() }
            .retrieve()
            .bodyToMono(String::class.java)
            .map { response -> parseTickerResponse(pair, response) }
    }

    fun getAccountBalance(): Mono<BalanceResponse> {
        return privateRequest(PRIVATE_BALANCE_PATH, emptyMap()).map(::parseBalanceResponse)
    }

    fun placeMarketOrder(pair: String, side: Side, volume: BigDecimal): Mono<TradeResponse> {
        return placeOrder(pair, side, OrderType.MARKET, volume, null)
    }

    fun placeLimitOrder(pair: String, side: Side, volume: BigDecimal, price: BigDecimal?): Mono<TradeResponse> {
        return placeOrder(pair, side, OrderType.LIMIT, volume, price)
    }

    private fun placeOrder(
        pair: String,
        side: Side,
        orderType: OrderType,
        volume: BigDecimal,
        price: BigDecimal?
    ): Mono<TradeResponse> {
        val params = LinkedHashMap<String, String>()
        params["pair"] = pair
        params["type"] = side.name.lowercase()
        params["ordertype"] = orderType.name.lowercase()
        params["volume"] = volume.toPlainString()
        if (price != null) {
            params["price"] = price.toPlainString()
        }
        return privateRequest(PRIVATE_ADD_ORDER_PATH, params)
            .map { body -> parseTradeResponse(pair, side, orderType, volume, price, body) }
    }

    private fun privateRequest(path: String, params: Map<String, String>): Mono<String> {
        val apiKey = requireCredentials(properties.apiKey, "KRAKEN_API_KEY")
        val secretKey = requireCredentials(properties.secretKey, "KRAKEN_SECRET_KEY")
        val nonce = nextNonce().toString()
        val postData = formEncode(params, nonce)
        val apiSign = sign(path, nonce, postData, secretKey)

        return krakenWebClient.post()
            .uri(path)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("API-Key", apiKey)
            .header("API-Sign", apiSign)
            .body(BodyInserters.fromValue(postData))
            .retrieve()
            .bodyToMono(String::class.java)
            .map(::ensureNoKrakenErrors)
    }

    private fun parseTickerResponse(pair: String, responseBody: String): PriceResponse {
        val root = readJson(responseBody)
        ensureNoKrakenErrors(root)
        val result = requireNode(root, "result")
        val pairNode = result.elements().asSequence().firstOrNull()
            ?: throw IllegalStateException("Kraken response missing result pair payload")
        val price = BigDecimal(requireNode(pairNode, "c")[0].asText())
        return PriceResponse(pair, price, Instant.now(), "Kraken price loaded successfully")
    }

    private fun parseBalanceResponse(responseBody: String): BalanceResponse {
        val root = readJson(responseBody)
        ensureNoKrakenErrors(root)
        val result = requireNode(root, "result")
        val balances = result.fields().asSequence()
            .map { entry -> AssetBalance(entry.key, BigDecimal(entry.value.asText()), BigDecimal.ZERO) }
            .toList()
        return BalanceResponse(balances, Instant.now(), "Kraken balance loaded successfully")
    }

    private fun parseTradeResponse(
        pair: String,
        side: Side,
        orderType: OrderType,
        volume: BigDecimal,
        price: BigDecimal?,
        responseBody: String
    ): TradeResponse {
        val root = readJson(responseBody)
        ensureNoKrakenErrors(root)
        val result = requireNode(root, "result")
        val txid = result.path("txid")
        val orderId = if (txid.isArray && !txid.isEmpty) txid[0].asText() else "UNKNOWN"
        val descr = result.path("descr")
        val message = descr.path("order").asText("Order submitted to Kraken")
        return TradeResponse(
            tradeId = orderId,
            orderId = orderId,
            symbol = pair,
            side = side,
            type = orderType,
            volume = volume,
            price = price,
            status = "PLACED",
            message = message,
            executedAt = Instant.now()
        )
    }

    private fun ensureNoKrakenErrors(responseBody: String): String {
        val root = readJson(responseBody)
        ensureNoKrakenErrors(root)
        return responseBody
    }

    private fun ensureNoKrakenErrors(root: JsonNode) {
        val errors = root.path("error")
        if (errors.isArray && !errors.isEmpty) {
            throw IllegalStateException("Kraken API error: $errors")
        }
    }

    private fun readJson(body: String): JsonNode {
        return try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to parse Kraken response", ex)
        }
    }

    private fun requireNode(node: JsonNode, field: String): JsonNode {
        val value = node.path(field)
        if (value.isMissingNode || value.isNull) {
            throw IllegalStateException("Kraken response missing field: $field")
        }
        return value
    }

    private fun nextNonce(): Long {
        val now = System.currentTimeMillis()
        return nonceSequence.updateAndGet { previous -> maxOf(now, previous + 1) }
    }

    private fun formEncode(params: Map<String, String>, nonce: String): String {
        return "nonce=${encode(nonce)}" + params.entries.joinToString("") { entry ->
            "&${encode(entry.key)}=${encode(entry.value)}"
        }
    }

    private fun sign(path: String, nonce: String, postData: String, secretKey: String): String {
        try {
            val secretBytes = Base64.getDecoder().decode(secretKey)
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hashed = sha256.digest((nonce + postData).toByteArray(StandardCharsets.UTF_8))
            val mac = Mac.getInstance("HmacSHA512")
            mac.init(SecretKeySpec(secretBytes, "HmacSHA512"))
            val pathBytes = path.toByteArray(StandardCharsets.UTF_8)
            val input = ByteArray(pathBytes.size + hashed.size)
            System.arraycopy(pathBytes, 0, input, 0, pathBytes.size)
            System.arraycopy(hashed, 0, input, pathBytes.size, hashed.size)
            return Base64.getEncoder().encodeToString(mac.doFinal(input))
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to sign Kraken request", ex)
        }
    }

    private fun requireCredentials(value: String?, envName: String): String {
        if (value.isNullOrBlank()) {
            throw IllegalStateException("$envName is not configured")
        }
        return value
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}
