package com.snowgears.shop.util;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PricePair{
	
	private double price;
	private double priceCombo;
	
	public PricePair(double price, double priceCombo) {
		this.price = price;
		this.priceCombo = priceCombo;
	}
	
}
