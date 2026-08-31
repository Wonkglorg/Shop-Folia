public final class Setting<T> {
    private final Class<T> type;
    private final Function<String, T> parser;
    private final T defaultValue;

    public Setting(
            Class<T> type,
            Function<String, T> parser,
            T defaultValue
    ) {
        this.type = type;
        this.parser = parser;
        this.defaultValue = defaultValue;
    }

    public T parse(String value) {
        return parser.apply(value);
    }

    public T getDefaultValue() {
        return defaultValue;
    }
}