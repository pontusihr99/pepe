package com.pepebot.service

import com.pepebot.dto.BalanceResponse
import com.pepebot.dto.OrderType
import com.pepebot.dto.PriceResponse
import com.pepebot.dto.TradeRequest
import com.pepebot.dto.TradeResponse
import com.pepebot.model.TradeLog
import com.pepebot.repository.TradeLogRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class TradingService(
    private val tradeLogRepository: TradeLogRepository,
    private val krakenClient: KrakenClient
) {
    private val clock: Clock = Clock.systemUTC()

    fun getCurrentPrice(symbol: String): PriceResponse {
        return try {
            krakenClient.getCurrentPrice(symbol).block(Duration.ofSeconds(10))
                ?: PriceResponse(symbol, BigDecimal.ZERO, Instant.now(clock), "No response from Kraken")
        } catch (ex: RuntimeException) {
            PriceResponse(symbol, BigDecimal.ZERO, Instant.now(clock), errorMessage(ex))
        }
    }

    fun getBalance(): BalanceResponse {
        return try {
            krakenClient.getAccountBalance().block(Duration.ofSeconds(10))
                ?: BalanceResponse(emptyList(), Instant.now(clock), "No response from Kraken")
        } catch (ex: IllegalStateException) {
            BalanceResponse(emptyList(), Instant.now(clock), errorMessage(ex))
        }
    }

    fun executeManualTrade(request: TradeRequest): TradeResponse {
        val now = Instant.now(clock)
        val tradeId = UUID.randomUUID().toString()
        val volume = requireNotNull(request.volume) { "Trade volume is required" }
        var message: String
        var status: String
        var orderId: String
        var executionPrice = request.price ?: BigDecimal.ZERO

        try {
            val response = if (request.type == OrderType.LIMIT) {
                krakenClient.placeLimitOrder(request.symbol, request.side, volume, request.price)
                    .block(Duration.ofSeconds(10))
            } else {
                krakenClient.placeMarketOrder(request.symbol, request.side, volume)
                    .block(Duration.ofSeconds(10))
            }

            if (response != null) {
                status = response.status
                orderId = response.orderId
                message = response.message
                executionPrice = response.price ?: executionPrice
            } else {
                status = "FAILED"
                orderId = "ERROR"
                message = "No response from Kraken"
            }
        } catch (ex: RuntimeException) {
            status = "FAILED"
            orderId = "ERROR"
            message = errorMessage(ex)
        }

        val log = TradeLog().apply {
            id = tradeId
            executedAt = now
            symbol = request.symbol
            side = request.side
            type = request.type
            this.volume = volume.setScale(8, RoundingMode.HALF_UP)
            price = executionPrice.setScale(12, RoundingMode.HALF_UP)
            this.status = status
            this.orderId = orderId
            this.message = message
        }
        tradeLogRepository.save(log)

        return TradeResponse(
            tradeId = tradeId,
            orderId = orderId,
            symbol = request.symbol,
            side = request.side,
            type = request.type,
            volume = volume,
            price = executionPrice,
            status = status,
            message = message,
            executedAt = now
        )
    }

    fun getRecentTrades(): List<TradeLog> {
        return tradeLogRepository.findTop20ByOrderByExecutedAtDesc()
            .sortedByDescending { it.executedAt }
    }

    private fun errorMessage(ex: RuntimeException): String {
        return ex.message ?: ex::class.simpleName ?: "RuntimeException"
    }
}
