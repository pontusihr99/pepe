package com.pepebot.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pepebot.config.KrakenProperties;
import com.pepebot.dto.AssetBalance;
import com.pepebot.dto.BalanceResponse;
import com.pepebot.dto.OrderType;
import com.pepebot.dto.PriceResponse;
import com.pepebot.dto.Side;
import com.pepebot.dto.TradeResponse;

import reactor.core.publisher.Mono;

@Service
public class KrakenClient {

	private static final String PUBLIC_TICKER_PATH = "/0/public/Ticker";
	private static final String PRIVATE_BALANCE_PATH = "/0/private/Balance";
	private static final String PRIVATE_ADD_ORDER_PATH = "/0/private/AddOrder";

	private final WebClient krakenWebClient;
	private final KrakenProperties properties;
	private final ObjectMapper objectMapper;
	private final AtomicLong nonceSequence = new AtomicLong(System.currentTimeMillis());

	public KrakenClient(WebClient krakenWebClient, KrakenProperties properties, ObjectMapper objectMapper) {
		this.krakenWebClient = krakenWebClient;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public Mono<PriceResponse> getCurrentPrice(String pair) {
		return krakenWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(PUBLIC_TICKER_PATH).queryParam("pair", pair).build())
				.retrieve()
				.bodyToMono(String.class)
				.map(response -> parseTickerResponse(pair, response));
	}

	public Mono<BalanceResponse> getAccountBalance() {
		return privateRequest(PRIVATE_BALANCE_PATH, Map.of())
				.map(this::parseBalanceResponse);
	}

	public Mono<TradeResponse> placeMarketOrder(String pair, Side side, BigDecimal volume) {
		return placeOrder(pair, side, OrderType.MARKET, volume, null);
	}

	public Mono<TradeResponse> placeLimitOrder(String pair, Side side, BigDecimal volume, BigDecimal price) {
		return placeOrder(pair, side, OrderType.LIMIT, volume, price);
	}

	private Mono<TradeResponse> placeOrder(String pair, Side side, OrderType orderType, BigDecimal volume, BigDecimal price) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("pair", pair);
		params.put("type", side.name().toLowerCase());
		params.put("ordertype", orderType.name().toLowerCase());
		params.put("volume", volume.toPlainString());
		if (price != null) {
			params.put("price", price.toPlainString());
		}
		return privateRequest(PRIVATE_ADD_ORDER_PATH, params)
				.map(body -> parseTradeResponse(pair, side, orderType, volume, price, body));
	}

	private Mono<String> privateRequest(String path, Map<String, String> params) {
		String apiKey = requireCredentials(properties.getApiKey(), "KRAKEN_API_KEY");
		String secretKey = requireCredentials(properties.getSecretKey(), "KRAKEN_SECRET_KEY");
		String nonce = String.valueOf(nextNonce());
		String postData = formEncode(params, nonce);
		String apiSign = sign(path, nonce, postData, secretKey);

		return krakenWebClient.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.header("API-Key", apiKey)
				.header("API-Sign", apiSign)
				.body(BodyInserters.fromValue(postData))
				.retrieve()
				.bodyToMono(String.class)
				.map(this::ensureNoKrakenErrors);
	}

	private PriceResponse parseTickerResponse(String pair, String responseBody) {
		JsonNode root = readJson(responseBody);
		ensureNoKrakenErrors(root);
		JsonNode result = requireNode(root, "result");
		JsonNode pairNode = result.elements().next();
		BigDecimal price = new BigDecimal(requireNode(pairNode, "c").get(0).asText());
		return new PriceResponse(pair, price, Instant.now(), "Kraken price loaded successfully");
	}

	private BalanceResponse parseBalanceResponse(String responseBody) {
		JsonNode root = readJson(responseBody);
		ensureNoKrakenErrors(root);
		JsonNode result = requireNode(root, "result");
		List<AssetBalance> balances = new ArrayList<>();
		for (var iterator = result.fields(); iterator.hasNext(); ) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			balances.add(new AssetBalance(entry.getKey(), new BigDecimal(entry.getValue().asText()), BigDecimal.ZERO));
		}
		return new BalanceResponse(balances, Instant.now(), "Kraken balance loaded successfully");
	}

	private TradeResponse parseTradeResponse(String pair, Side side, OrderType orderType, BigDecimal volume, BigDecimal price, String responseBody) {
		JsonNode root = readJson(responseBody);
		ensureNoKrakenErrors(root);
		JsonNode result = requireNode(root, "result");
		JsonNode txid = result.path("txid");
		String orderId = txid.isArray() && !txid.isEmpty() ? txid.get(0).asText() : "UNKNOWN";
		JsonNode descr = result.path("descr");
		String message = descr.path("order").asText("Order submitted to Kraken");
		return new TradeResponse(
				orderId,
				orderId,
				pair,
				side,
				orderType,
				volume,
				price,
				"PLACED",
				message,
				Instant.now());
	}

	private String ensureNoKrakenErrors(String responseBody) {
		JsonNode root = readJson(responseBody);
		ensureNoKrakenErrors(root);
		return responseBody;
	}

	private void ensureNoKrakenErrors(JsonNode root) {
		JsonNode errors = root.path("error");
		if (errors.isArray() && !errors.isEmpty()) {
			throw new IllegalStateException("Kraken API error: " + errors);
		}
	}

	private JsonNode readJson(String body) {
		try {
			return objectMapper.readTree(body);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to parse Kraken response", ex);
		}
	}

	private JsonNode requireNode(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull()) {
			throw new IllegalStateException("Kraken response missing field: " + field);
		}
		return value;
	}

	private long nextNonce() {
		long now = System.currentTimeMillis();
		return nonceSequence.updateAndGet(previous -> Math.max(now, previous + 1));
	}

	private String formEncode(Map<String, String> params, String nonce) {
		return "nonce=" + encode(nonce) + params.entrySet().stream()
				.map(entry -> "&" + encode(entry.getKey()) + "=" + encode(entry.getValue()))
				.collect(Collectors.joining());
	}

	private String sign(String path, String nonce, String postData, String secretKey) {
		try {
			byte[] secretBytes = Base64.getDecoder().decode(secretKey);
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] hashed = sha256.digest((nonce + postData).getBytes(StandardCharsets.UTF_8));
			Mac mac = Mac.getInstance("HmacSHA512");
			mac.init(new SecretKeySpec(secretBytes, "HmacSHA512"));
			byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
			byte[] input = new byte[pathBytes.length + hashed.length];
			System.arraycopy(pathBytes, 0, input, 0, pathBytes.length);
			System.arraycopy(hashed, 0, input, pathBytes.length, hashed.length);
			return Base64.getEncoder().encodeToString(mac.doFinal(input));
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to sign Kraken request", ex);
		}
	}

	private String requireCredentials(String value, String envName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(envName + " is not configured");
		}
		return value;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
