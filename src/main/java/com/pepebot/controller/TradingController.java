package com.pepebot.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pepebot.dto.BalanceResponse;
import com.pepebot.dto.PriceResponse;
import com.pepebot.dto.TradeRequest;
import com.pepebot.dto.TradeResponse;
import com.pepebot.model.TradeLog;
import com.pepebot.service.TradingService;

@RestController
@RequestMapping("/api")
public class TradingController {

	private final TradingService tradingService;

	public TradingController(TradingService tradingService) {
		this.tradingService = tradingService;
	}

	@GetMapping("/price")
	public PriceResponse getPrice(@RequestParam(defaultValue = "PEPEEUR") String symbol) {
		return tradingService.getCurrentPrice(symbol);
	}

	@GetMapping("/balance")
	public BalanceResponse getBalance() {
		return tradingService.getBalance();
	}

	@GetMapping("/trades")
	public List<TradeLog> getRecentTrades() {
		return tradingService.getRecentTrades();
	}

	@PostMapping("/trade")
	public TradeResponse trade(@Valid @RequestBody TradeRequest request) {
		return tradingService.executeManualTrade(request);
	}
}
