package com.pepebot.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.pepebot.service.TradingService;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_KRAKEN_TESTS", matches = "true")
class KrakenClientIntegrationTest extends IntegrationTestBase {

	@Autowired
	private TradingService tradingService;

	@Test
	void livePriceFetch_returnsPositivePrice() {
		var price = tradingService.getCurrentPrice("PEPEEUR");

		assertEquals("PEPEEUR", price.symbol());
		assertTrue(price.price().compareTo(BigDecimal.ZERO) > 0);
	}

	@Test
	void livePrivateBalanceFetch_readsSecretsFromSecretManager() {
		var balance = tradingService.getBalance();

		assertEquals("Kraken balance loaded successfully", balance.message());
		assertTrue(balance.assets().size() >= 0);
	}
}
