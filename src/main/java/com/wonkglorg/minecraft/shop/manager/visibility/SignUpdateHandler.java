/**
 * Handles player-specific sign updating and changes.
 */
public class SignUpdateHandler implements ShopVisibilityListener{
	
	private final Main plugin;
	
	private final Map<UUID, Map<UUID, List<Component>>> displayedSignLines = new ConcurrentHashMap<>();
	
	public SignVisibilityHandler(Main plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		updateSign(player, shop);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		//nothing needs changing
	}
	
	@Override
	public void onShopRefresh(Player player, AbstractShop shop) {
		updateSign(player, shop);
	}
	
	//todo how to best cache values
	//todo how do I force refresh the shop sign for everyone
	private void updateSign(Player player, AbstractShop shop) {
		if(!player.isOnline()){
			return;
		}
		
		Location location = shop.getContainerLocation();
		
		if(location == null || location.getWorld() == null){
			return;
		}
		
		// Build the text specifically for this player.
		String[] lines = getSignLines(player, shop);
		
		sendSignUpdate(player, location, lines);
	}
	
	private String[] getSignLines(Player player, AbstractShop shop) {
		PlayerProfile profile = PlayerManager.getProfile(player.getUniqueId());
		
		shop.getClientShopState();
		
		int purchases = profile.getPurchaseCount(shop.getUuid());
		
		long lastPurchase = profile.getLastPurchaseTime(shop.getUuid());
		
		return new String[]{shop.getItem(), "Price: " + shop.getPrice(), "Bought: " + purchases, "Last: " + lastPurchase};
	}
	
	private void sendSignUpdate(Player player, Location location, String[] lines) {
		// Paper/client-specific implementation
	}
	
	public void refreshShop(AbstractShop shop) {
		for(UUID playerId : visibilityManager.getPlayersSeeingShop(shop)){
			
			Player player = Bukkit.getPlayer(playerId);
			
			if(player != null && player.isOnline()){
				updateSign(player, shop);
			}
		}
	}
	
	public void updateSign(Player player, AbstractShop shop, boolean forceUpdate) {
		if(!player.isOnline()){
			return;
		}
		
		Location location = shop.getSignLocation();
		
		if(location == null || location.getWorld() == null){
			return;
		}
		
		if(!location.getChunk().isLoaded()){
			return;
		}
		
		List<Component> newLines = getSignLines(player, shop);
		
		UUID playerId = player.getUniqueId();
		UUID shopId = shop.getUuid();
		
		Map<UUID, List<Component>> playerCache = displayedSignLines.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
		
		List<Component> oldLines = playerCache.get(shopId);
		
		if(!forceUpdate && newLines.equals(oldLines)){
			return;
		}
		
		playerCache.put(shopId, List.copyOf(newLines));
		
		sendSignUpdate(player, location, newLines);
	}
	
	private void sendSignUpdate(Player player, Location location, RenderedSign state) {
		if(!(location.getBlock().getState() instanceof Sign realSign)){
			return;
		}
		
		Sign virtualSign = (Sign) location.getBlock().getBlockData().createBlockState();
		
		virtualSign.setWaxed(state.waxed());
		
		SignSide realFront = realSign.getSide(Side.FRONT);
		SignSide virtualFront = virtualSign.getSide(Side.FRONT);
		
		virtualFront.setColor(state.color());
		virtualFront.setGlowingText(state.glowing());
		
		for(int i = 0; i < 4; i++){
			virtualFront.line(i, state.lines().get(i));
		}
		
		player.sendBlockUpdate(location, virtualSign);
	}
}