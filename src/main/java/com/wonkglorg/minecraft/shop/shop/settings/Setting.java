public final class Setting<T>{
	private final Class<T> type;
	private final Function<String, T> parser;
	private final Supplier<T> defaultValue;
	private final Supplier<Boolean> enabled;
	
	public Setting(Class<T> type, Function<String, T> parser, Supplier<T> defaultValue, Supplier<Boolean> enabled) {
		this.type = type;
		this.parser = parser;
		this.defaultValue = defaultValue;
		this.enabled = enabled;
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