package com.pepebot.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TradeRequest {

	@Size(min = 3, max = 32)
	private String symbol = "PEPEEUR";

	@NotNull
	private Side side = Side.BUY;

	@NotNull
	private OrderType type = OrderType.MARKET;

	@NotNull
	@DecimalMin("0.00000001")
	private BigDecimal volume;

	@DecimalMin("0.00000001")
	private BigDecimal price;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}

	public OrderType getType() {
		return type;
	}

	public void setType(OrderType type) {
		this.type = type;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public void setVolume(BigDecimal volume) {
		this.volume = volume;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}
}
