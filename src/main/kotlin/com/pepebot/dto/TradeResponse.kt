package com.pepebot.dto

import java.math.BigDecimal
import java.time.Instant

data class TradeResponse(
    val tradeId: String,
    val orderId: String,
    val symbol: String,
    val side: Side,
    val type: OrderType,
    val volume: BigDecimal,
    val price: BigDecimal?,
    val status: String,
    val message: String,
    val executedAt: Instant
)
