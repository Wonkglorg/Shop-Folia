public enum Settings{
	PURCHASE_COOLDOWN(Long.class, s -> Long.parse(s)),
	PURCHASE_LIMIT(Integer.class,s ->Long.parse(s));
	
	Settings(Class<T> clazz, Function<String, T> objectFunction) {
	}
}