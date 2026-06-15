package com.pepebot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pepebot.dto.AssetBalance;
import com.pepebot.dto.BalanceResponse;
import com.pepebot.dto.OrderType;
import com.pepebot.dto.PriceResponse;
import com.pepebot.dto.Side;
import com.pepebot.dto.TradeRequest;
import com.pepebot.dto.TradeResponse;
import com.pepebot.model.TradeLog;
import com.pepebot.repository.TradeLogRepository;

@Service
public class TradingService {

	private final TradeLogRepository tradeLogRepository;
	private final KrakenClient krakenClient;
	private final Clock clock = Clock.systemUTC();

	public TradingService(TradeLogRepository tradeLogRepository, KrakenClient krakenClient) {
		this.tradeLogRepository = tradeLogRepository;
		this.krakenClient = krakenClient;
	}

	public PriceResponse getCurrentPrice(String symbol) {
		try {
			return krakenClient.getCurrentPrice(symbol).block(Duration.ofSeconds(10));
		} catch (RuntimeException ex) {
			return new PriceResponse(symbol, BigDecimal.ZERO, Instant.now(clock), errorMessage(ex));
		}
	}

	public BalanceResponse getBalance() {
		try {
			return krakenClient.getAccountBalance().block(Duration.ofSeconds(10));
		} catch (IllegalStateException ex) {
			return new BalanceResponse(List.of(), Instant.now(clock), errorMessage(ex));
		}
	}

	public TradeResponse executeManualTrade(TradeRequest request) {
		Instant now = Instant.now(clock);
		String tradeId = UUID.randomUUID().toString();
		String message;
		String status;
		String orderId;
		BigDecimal executionPrice = request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO;

		try {
			TradeResponse response = request.getType() == OrderType.LIMIT
					? krakenClient.placeLimitOrder(request.getSymbol(), request.getSide(), request.getVolume(), request.getPrice())
							.block(Duration.ofSeconds(10))
					: krakenClient.placeMarketOrder(request.getSymbol(), request.getSide(), request.getVolume())
							.block(Duration.ofSeconds(10));
			status = response.status();
			orderId = response.orderId();
			message = response.message();
			executionPrice = response.price() != null ? response.price() : executionPrice;
		} catch (RuntimeException ex) {
			status = "FAILED";
			orderId = "ERROR";
			message = errorMessage(ex);
		}

		TradeLog log = new TradeLog();
		log.setId(tradeId);
		log.setExecutedAt(now);
		log.setSymbol(request.getSymbol());
		log.setSide(request.getSide());
		log.setType(request.getType());
		log.setVolume(request.getVolume().setScale(8, RoundingMode.HALF_UP));
		log.setPrice(executionPrice.setScale(12, RoundingMode.HALF_UP));
		log.setStatus(status);
		log.setOrderId(orderId);
		log.setMessage(message);
		tradeLogRepository.save(log);

		return new TradeResponse(
				tradeId,
				orderId,
				request.getSymbol(),
				request.getSide(),
				request.getType(),
				request.getVolume(),
				executionPrice,
				status,
				message,
				now);
	}

	public List<TradeLog> getRecentTrades() {
		return tradeLogRepository.findTop20ByOrderByExecutedAtDesc()
				.stream()
				.sorted(Comparator.comparing(TradeLog::getExecutedAt).reversed())
				.toList();
	}

	private String errorMessage(RuntimeException ex) {
		return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
	}
}
