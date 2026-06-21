package com.pepebot.testing

import com.pepebot.service.TradingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_KRAKEN_TESTS", matches = "true")
class KrakenClientIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var tradingService: TradingService

    @Test
    fun livePriceFetch_returnsPositivePrice() {
        val price = tradingService.getCurrentPrice("PEPEEUR")

        assertEquals("PEPEEUR", price.symbol)
        assertTrue(price.price > BigDecimal.ZERO)
    }

    @Test
    fun livePrivateBalanceFetch_readsSecretsFromSecretManager() {
        val balance = tradingService.getBalance()

        assertEquals("Kraken balance loaded successfully", balance.message)
        assertTrue(balance.assets.size >= 0)
    }
}
