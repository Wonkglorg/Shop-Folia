package com.wonkglorg.minecraft.shop.manager.client;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import com.wonkglorg.minecraft.shop.manager.PlayerManager;
import com.wonkglorg.minecraft.shop.manager.player.PlayerProfile;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopStateClient;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player-specific sign updating and changes.
 */
public class SignUpdateHandler implements ShopClientListener{
	private final Map<UUID, Map<UUID, ShopStateClient>> lastSignStates = new HashMap<>();
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		updateSign(player, shop);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		//nothing needs changing
	}
	
	@Override
	public void clearData(UUID player) {
		lastSignStates.remove(player);
	}
	
	@Override
	public void onShopCleanup(Player player, AbstractShop shop) {
		//do nothing to save on packet sending, but if need be send original shop sign via method below
		//player.sendBlockChange(shop.getSignLocation(), shop.getSignLocation().getBlock().getBlockData());
		//clear user data so the shop will be properly reloaded when shopEnter is next caller
		var map = lastSignStates.get(player.getUniqueId());
		if(map != null){
			map.remove(shop.getId());
		}
	}
	
	@Override
	public void onShopCleanup(Player player) {
		lastSignStates.remove(player.getUniqueId());
	}
	
	@Override
	public boolean needsUpdate(Player player, AbstractShop shop) {
		Map<UUID, ShopStateClient> map = lastSignStates.get(player.getUniqueId());
		if(map == null){
			return true;
		}
		ShopStateClient state = map.get(shop.getId());
		if(state == null){
			return true;
		}
		return shop.getClientShopState(player) != state;
	}
	
	private void updateSign(Player player, AbstractShop shop) {
		if(!player.isOnline()){
			return;
		}
		
		UUID playerId = player.getUniqueId();
		PlayerProfile profile = PlayerManager.getOnlineProfileIfCached(playerId);
		
		if(profile == null){
			return;
		}
		
		//if nothing changed about the state just return
		ShopStateClient currentSignState = shop.getClientShopState(profile);
		UUID shopId = shop.getId(); // use whatever your AbstractShop shop UUID getter is
		
		ShopStateClient previous = lastSignStates.computeIfAbsent(playerId, _ -> new HashMap<>()).get(shopId);
		//nothing changed no update needed
		if(currentSignState == previous){
			return;
		}
		
		Location location = shop.getSignLocation();
		
		ShopPlugin.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(location, () -> {
			if(!player.isOnline()){
				return;
			}
			
			WallSign realSign = shop.getSign();
			
			if(realSign == null){
				return;
			}
			
			if(!(realSign.createBlockState() instanceof Sign sign)){
				return;
			}
			
			List<Component> lines = shop.getSignLines(profile);
			
			SignSide front = sign.getSide(Side.FRONT);
			
			for(int i = 0; i < 4; i++){
				front.line(i, lines.get(i));
			}
			
			front.setGlowingText(ShopPlugin.getPlugin().getSettingsConfig().isSignGlowingSignText());
			sign.setWaxed(ShopPlugin.getPlugin().getSettingsConfig().isSignWaxed());
			
			player.sendBlockUpdate(location, sign);
			lastSignStates.computeIfAbsent(playerId, _ -> new HashMap<>()).put(shopId, currentSignState);
		}, 1);
	}
	
	public static @NonNull List<Component> getComponents(AbstractShop shop, String langKey) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = langManager().request(langKey + "." + i);
			
			request.replace("%item%",() -> ItemNameUtil.getName(shop.getItemStack()))
				   .replace("%stock-state%",shop.getShopState())
				   .replace("%amount%",shop.getAmount())
				   .replace("%price%",shop.getPriceFormatted())
				   .replace("%owner%",shop::getOwnerNameFormatted)
				   .replace("%stock%",shop.getStock());
			
			ItemStack barterStack = shop.getSecondaryItemStack();
			if(barterStack!=null){
				request.replace("%barter-item%",() -> ItemNameUtil.getName(shop.getSecondaryItemStack()));
			}
			lines.add(request.toSingleComponent());
			//@formatter:on
		}
		return lines;
	}
	
	/**
	 * The shop lines defined in the lang config
	 *
	 * @return a list with a capacity of 4
	 */
	public static List<Component> getSignLinesTimeout() {
		List<Component> lines = new ArrayList<>(4);
		
		for(var i = 1; i < 5; i++){
			lines.add(langManager().request("sign.text.timeout." + i).toSingleComponent());
		}
		return lines;
	}
	
	/**
	 * @return a default set of sign text to assign when a shop loads so the shop sign looks similar to what the clients state would be in case something loads slow.
	 */
	public static List<Component> getDefaultSignLines(AbstractShop shop) {
		return getComponents(shop, "sign.text." + shop.getType().toString() + ".in-stock");
	}
}