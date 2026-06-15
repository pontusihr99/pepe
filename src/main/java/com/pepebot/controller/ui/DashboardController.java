package com.pepebot.controller.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import com.pepebot.dto.TradeRequest;
import com.pepebot.service.TradingService;

@Controller
@RequestMapping
public class DashboardController {

	private final TradingService tradingService;

	public DashboardController(TradingService tradingService) {
		this.tradingService = tradingService;
	}

	@GetMapping("/")
	public String root() {
		return "redirect:/dashboard";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		return populateDashboard(model, new TradeRequest(), null);
	}

	@PostMapping("/dashboard/trade")
	public String submitTrade(@Valid @ModelAttribute("tradeRequest") TradeRequest tradeRequest,
			BindingResult bindingResult, Model model) {
		String message = null;
		if (!bindingResult.hasErrors()) {
			message = tradingService.executeManualTrade(tradeRequest).message();
			tradeRequest = new TradeRequest();
		}
		return populateDashboard(model, tradeRequest, message);
	}

	private String populateDashboard(Model model, TradeRequest tradeRequest, String message) {
		model.addAttribute("price", tradingService.getCurrentPrice(tradeRequest.getSymbol()));
		model.addAttribute("balance", tradingService.getBalance());
		model.addAttribute("recentTrades", tradingService.getRecentTrades());
		model.addAttribute("tradeRequest", tradeRequest);
		model.addAttribute("message", message);
		return "dashboard";
	}
}
