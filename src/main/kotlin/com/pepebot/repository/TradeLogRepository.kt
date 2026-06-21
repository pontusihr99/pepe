package com.pepebot.repository

import com.pepebot.model.TradeLog
import org.springframework.data.jpa.repository.JpaRepository

interface TradeLogRepository : JpaRepository<TradeLog, String> {
    fun findTop20ByOrderByExecutedAtDesc(): List<TradeLog>
}
