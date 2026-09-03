package com.wonkglorg.minecraft.shop.shop.settings;

import lombok.Getter;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Setting<T>{
	@Getter
	private final String key;
	@Getter
	private final Class<T> type;
	private final Function<String, T> parser;
	private final Supplier<T> defaultValue;
	private final Supplier<Boolean> enabled;
	
	public Setting(String key, Class<T> type, Function<String, T> parser, Supplier<T> defaultValue, Supplier<Boolean> enabled) {
		this.key = key;
		this.type = type;
		this.parser = parser;
		this.defaultValue = defaultValue;
		this.enabled = enabled;
		Settings.ALL_SETTINGS.put(key, this);
	}
	
	public T parse(String value) {
		return parser.apply(value);
	}
	
	public T getDefaultValue() {
		return defaultValue.get();
	}
	
	public boolean isEnabled() {
		return enabled.get();
	}
	
}