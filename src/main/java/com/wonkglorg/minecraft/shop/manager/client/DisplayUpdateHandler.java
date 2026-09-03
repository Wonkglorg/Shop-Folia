package com.wonkglorg.minecraft.shop.manager.client;

import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.display.AbstractDisplay;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles display visibility
 */
public class DisplayUpdateHandler implements ShopClientListener{
	
	/**
	 * a map of the player uuid and all shop uuids / their known loaded entity id's
	 */
	protected Map<UUID, Map<UUID, List<Integer>>> shopDisplayEntities = new ConcurrentHashMap<>(); //player UUID. Shop UUID, display entity ids
	
	@Override
	public void onShopEnter(Player player, AbstractShop shop) {
		shopDisplayEntities.computeIfAbsent(player.getUniqueId(), _ -> new ConcurrentHashMap<>()).put(shop.getId(), shop.getDisplay().spawn(player));
	}
	
	@Override
	public void onShopLeave(Player player, AbstractShop shop) {
		onShopCleanup(player, shop);
	}
	
	@Override
	public void clearData(UUID player) {
		shopDisplayEntities.remove(player);
	}
	
	@Override
	public void onShopCleanup(Player player, AbstractShop shop) {
		Map<UUID, List<Integer>> map = shopDisplayEntities.get(player.getUniqueId());
		if(map == null){
			return;
		}
		AbstractDisplay.remove(player, map.remove(shop.getId()));
	}
	
	@Override
	public void onShopCleanup(Player player) {
		Map<UUID, List<Integer>> map = shopDisplayEntities.remove(player.getUniqueId());
		if(map == null){
			return;
		}
		for(var entry : map.values()){
			AbstractDisplay.remove(player, entry);
		}
	}
}