public final class Settings{
	/**
	 * Cooldown applied after each purchase (per player)
	 */
	public static final Setting<Long> PURCHASE_COOLDOWN = new Setting<>(Long.class, Long::parseLong, 0L);
	/**
	 * Limit on how often a player can buy from this shop before permanently being unavailable
	 */
	public static final Setting<Integer> PURCHASE_LIMIT = new Setting<>(Integer.class, Integer::parseInt, 0);
	
	
	public static
}