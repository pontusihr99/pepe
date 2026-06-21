package com.pepebot.controller.ui

import com.pepebot.dto.TradeRequest
import com.pepebot.service.TradingService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping
class DashboardController(
    private val tradingService: TradingService
) {
    @GetMapping("/")
    fun root(): String {
        return "redirect:/dashboard"
    }

    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        return populateDashboard(model, TradeRequest(), null)
    }

    @PostMapping("/dashboard/trade")
    fun submitTrade(
        @Valid @ModelAttribute("tradeRequest") tradeRequest: TradeRequest,
        bindingResult: BindingResult,
        model: Model
    ): String {
        var nextTradeRequest = tradeRequest
        var message: String? = null
        if (!bindingResult.hasErrors()) {
            message = tradingService.executeManualTrade(tradeRequest).message
            nextTradeRequest = TradeRequest()
        }
        return populateDashboard(model, nextTradeRequest, message)
    }

    private fun populateDashboard(model: Model, tradeRequest: TradeRequest, message: String?): String {
        model.addAttribute("price", tradingService.getCurrentPrice(tradeRequest.symbol))
        model.addAttribute("balance", tradingService.getBalance())
        model.addAttribute("recentTrades", tradingService.getRecentTrades())
        model.addAttribute("tradeRequest", tradeRequest)
        model.addAttribute("message", message)
        return "dashboard"
    }
}
