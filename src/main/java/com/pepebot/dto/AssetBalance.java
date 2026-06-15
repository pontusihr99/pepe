package com.pepebot.dto;

import java.math.BigDecimal;

public record AssetBalance(String asset, BigDecimal free, BigDecimal locked) {
}
