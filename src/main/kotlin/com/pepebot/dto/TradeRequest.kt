package com.pepebot.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

class TradeRequest {
    @field:Size(min = 3, max = 32)
    var symbol: String = "PEPEEUR"

    @field:NotNull
    var side: Side = Side.BUY

    @field:NotNull
    var type: OrderType = OrderType.MARKET

    @field:NotNull
    @field:DecimalMin("0.00000001")
    var volume: BigDecimal? = null

    @field:DecimalMin("0.00000001")
    var price: BigDecimal? = null
}
