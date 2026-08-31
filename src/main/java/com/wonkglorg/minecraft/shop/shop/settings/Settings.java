public final class Settings{
	//todo allow setting default values via config that all shops inherit.
	
	
	/**
	 * Cooldown applied after each purchase (per player)
	 */
	public static final Setting<Long> PURCHASE_COOLDOWN = new Setting<>(Long.class, Long::parseLong, 0L);
	/**
	 * Limit on how often a player can buy from this shop before permanently being unavailable
	 */
	public static final Setting<Integer> PURCHASE_LIMIT = new Setting<>(Integer.class, Integer::parseInt, 0);
	/**
	 * If the shop owner should inform the shop owner if the shop is out of stock
	 */
	public static final Setting<Boolean> OUT_OF_STOCK_NOTIFICATION = new Setting<>(Integer.class, Integer::parseInt, 0);
	/**
	 * If the shop owner should be notified about this shop doing transaction
	 */
	public static final Setting<Boolean> TRANSACTION_NOTIFICATION= new Setting<>(Integer.class, Integer::parseInt, 0);
	
	
	public static
}