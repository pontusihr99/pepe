package com.pepebot.model

import com.pepebot.dto.OrderType
import com.pepebot.dto.Side
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "trade_log")
open class TradeLog {
    @Id
    open var id: String = ""

    @Column(nullable = false)
    open var executedAt: Instant = Instant.EPOCH

    @Column(nullable = false)
    open var symbol: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var side: Side = Side.BUY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var type: OrderType = OrderType.MARKET

    @Column(nullable = false, precision = 38, scale = 18)
    open var volume: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false, precision = 38, scale = 18)
    open var price: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false)
    open var status: String = ""

    @Column(nullable = false)
    open var orderId: String = ""

    @Column(nullable = false)
    open var message: String = ""
}
