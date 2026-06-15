package com.pepebot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pepebot.model.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, String> {

	List<TradeLog> findTop20ByOrderByExecutedAtDesc();
}
