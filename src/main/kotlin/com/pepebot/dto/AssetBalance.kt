package com.pepebot.dto

import java.math.BigDecimal

data class AssetBalance(
    val asset: String,
    val free: BigDecimal,
    val locked: BigDecimal
)
