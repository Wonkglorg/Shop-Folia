package com.wonkglorg.minecraft.shop.manager.visibility;

import com.wonkglorg.minecraft.config.LangManager;
import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.Main;
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
public class SignUpdateHandler implements ShopVisibilityListener{
	private final Map<UUID, Map<UUID, SignState>> lastSignStates = new HashMap<>();
	
	private record SignState(ShopStateClient state, int usage, long lastUsed){}
	
	private static final LangManager lang = Main.getPlugin().getLangManager();
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		updateSign(player, shop, false);
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		//nothing needs changing
	}
	
	@Override
	public void onShopRefresh(Player player, AbstractShop shop) {
		updateSign(player, shop, true);
	}
	
	private void updateSign(Player player, AbstractShop shop, boolean force) {
		if(!player.isOnline()){
			return;
		}
		
		PlayerProfile profile = PlayerManager.getOnlineProfileIfCached(player.getUniqueId());
		
		if(profile == null){
			return;
		}
		
		//if nothing changed about the state just return
		if(!force){
			if(!hasSignStateChanged(profile, shop)){
				return;
			}
		}
		
		Location location = shop.getSignLocation();
		
		Main.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(location, () -> {
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
			
			boolean glowing = Main.getPlugin().getSettingsConfig().isSignGlowingSignText();
			
			front.setGlowingText(glowing);
			sign.setWaxed(glowing);
			
			player.sendBlockUpdate(location, sign);
		}, 1);
	}
	
	public void refreshShop(AbstractShop shop) {
		for(var player : getPlayersSeeingShop(shop)){
			if(player != null && player.isOnline()){
				updateSign(player, shop, true);
			}
		}
	}
	
	public static @NonNull List<Component> getComponents(AbstractShop shop, String langKey) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = lang.request(langKey + "." + i);
			
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
			lines.add(lang.request("sign.text.timeout." + i).toSingleComponent());
		}
		return lines;
	}
	
	private boolean hasSignStateChanged(PlayerProfile player, AbstractShop shop) {
		ShopStateClient state = shop.getClientShopState(player);
		int usage = shop.usageTimes(player);
		long lastUsed = shop.lastUsedTime(player);
		
		SignState current = new SignState(state, usage, lastUsed);
		
		UUID playerId = player.getUuid();
		UUID shopId = shop.getId(); // use whatever your AbstractShop shop UUID getter is
		
		SignState previous = lastSignStates.computeIfAbsent(playerId, _ -> new HashMap<>()).put(shopId, current);
		
		return !current.equals(previous);
	}
	
	/**
	 * @return a default set of sign text to assign when a shop loads so the shop sign looks similar to what the clients state would be in case something loads slow.
	 */
	public static List<Component> getDefaultSignLines(AbstractShop shop) {
		return getComponents(shop, "sign.text." + shop.getType().toString() + ".in-stock");
	}
}