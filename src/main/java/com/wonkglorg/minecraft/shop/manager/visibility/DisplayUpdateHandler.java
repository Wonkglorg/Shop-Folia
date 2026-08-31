/**
 * Handles display visibility
 */
public class DisplayUpdateHandler implements ShopVisibilityListener{
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		shop.getDisplay().spawn(player);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		shop.getDisplay().remove(player);
	}
	
	@Override
	public void onShopRefresh(Player player, AbstractShop shop) {
		shop.getDisplay().remove(player);
		shop.getDisplay().spawn(player);
	}
}