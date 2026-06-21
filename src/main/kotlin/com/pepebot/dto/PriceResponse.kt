package com.pepebot.dto

import java.math.BigDecimal
import java.time.Instant

data class PriceResponse(
    val symbol: String,
    val price: BigDecimal,
    val updatedAt: Instant,
    val message: String
)
