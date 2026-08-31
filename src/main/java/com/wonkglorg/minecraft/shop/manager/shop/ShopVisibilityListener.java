public interface ShopVisibilityListener{
	
	/**
	 * Called when a shop becomes visible to a player.
	 */
	void onShopEnter(Player player, AbstractShop shop);
	
	/**
	 * Called when a shop is no longer visible to a player.
	 */
	void onShopLeave(Player player, AbstractShop shop);
}