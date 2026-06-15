package com.pepebot.dto;

import java.time.Instant;
import java.util.List;

public record BalanceResponse(List<AssetBalance> assets, Instant updatedAt, String message) {
}
