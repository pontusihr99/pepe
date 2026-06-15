package com.pepebot.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
		String tradeId,
		String orderId,
		String symbol,
		Side side,
		OrderType type,
		BigDecimal volume,
		BigDecimal price,
		String status,
		String message,
		Instant executedAt) {
}
