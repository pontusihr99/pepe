package com.pepebot.dto

import java.time.Instant

data class BalanceResponse(
    val assets: List<AssetBalance>,
    val updatedAt: Instant,
    val message: String
)
