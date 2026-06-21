package com.pepebot.controller

import com.pepebot.dto.BalanceResponse
import com.pepebot.dto.PriceResponse
import com.pepebot.dto.TradeRequest
import com.pepebot.dto.TradeResponse
import com.pepebot.model.TradeLog
import com.pepebot.service.TradingService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TradingController(
    private val tradingService: TradingService
) {
    @GetMapping("/price")
    fun getPrice(@RequestParam(defaultValue = "PEPEEUR") symbol: String): PriceResponse {
        return tradingService.getCurrentPrice(symbol)
    }

    @GetMapping("/balance")
    fun getBalance(): BalanceResponse {
        return tradingService.getBalance()
    }

    @GetMapping("/trades")
    fun getRecentTrades(): List<TradeLog> {
        return tradingService.getRecentTrades()
    }

    @PostMapping("/trade")
    fun trade(@Valid @RequestBody request: TradeRequest): TradeResponse {
        return tradingService.executeManualTrade(request)
    }
}
