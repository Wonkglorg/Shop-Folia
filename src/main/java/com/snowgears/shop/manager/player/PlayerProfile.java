package com.snowgears.shop.manager.player;

import com.snowgears.shop.Constants;
import com.snowgears.shop.Shop;
import static com.snowgears.shop.manager.PlayerManager.loadfromFile;
import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.shop.ShopType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A players profile holding shop specific data
 */
public abstract class PlayerProfile{
	@Getter
	private final UUID uuid;
	@Getter
	private final OfflinePlayer offlinePlayer;
	@Getter
	@Setter
	private boolean notifyUser;
	@Getter
	@Setter
	private boolean notifyOwner;
	@Getter
	@Setter
	private boolean notifyStock;
	
	@Getter
	private final Map<ShopType, List<AbstractShop>> ownedShops = new ConcurrentHashMap<>();
	
	protected PlayerProfile(OfflinePlayer offlinePlayer) {
		for(var type : ShopType.values()){
			ownedShops.put(type, new ArrayList<>());
		}
		
		this.uuid = offlinePlayer.getUniqueId();
		this.offlinePlayer = offlinePlayer;
		for(var shop : getShops(offlinePlayer.getUniqueId())){
			ownedShops.get(shop.getType()).add(shop);
		}
		loadfromFile(this);
	}
	
	/**
	 * Creates a profile of an online player
	 */
	public static OnlinePlayerProfile online(Player player) {
		return new OnlinePlayerProfile(player);
	}
	
	/**
	 * Creates a profile of an offline player
	 */
	public static OfflinePlayerProfile offline(OfflinePlayer offlinePlayer) {
		return new OfflinePlayerProfile(offlinePlayer);
	}
	
	/**
	 * If the user either has the operator permission or is op, giving them full access to all features of the plugin
	 */
	public static boolean isOperator(Permissible player) {
		return player.isOp() || !player.hasPermission(Constants.SHOP_PERMISSION_OPERATOR);
	}
	
	/**
	 * If the user is allowed to create a shop of this type, this does NOT enforce shop build limit
	 */
	public static boolean isAllowedToCreateShopType(Permissible player, ShopType type) {
		return player.hasPermission("shop.create." + type.toString().toLowerCase()) || player.hasPermission("shop.create") || isOperator(player);
	}
	
	/**
	 * If the user is allowed to create a shop of any type, to find out what specific type they can create use {@link #isAllowedToCreateShopType(Permissible, ShopType)} instead
	 */
	public static boolean isAllowedToCreateShop(Permissible player) {
		if(isOperator(player)){
			return true;
		}
		if(player.hasPermission("shop.create")){
			return true;
		}
		
		for(ShopType shopType : ShopType.values()){
			if(player.hasPermission("shop.create." + shopType.toString().toLowerCase())){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 *
	 * @param player the player to check.
	 * @return all shop types the player is allowed to build
	 */
	public static List<ShopType> getBuildableShopTypes(Permissible player) {
		List<ShopType> typeList = new ArrayList<>(Arrays.asList(ShopType.values()));
		if(isOperator(player)){
			return typeList;
		} else {
			typeList.remove(ShopType.GAMBLE);
		}
		
		if(player.hasPermission("shop.create")){
			return typeList;
		}
		
		Iterator<ShopType> typeIterator = typeList.iterator();
		while(typeIterator.hasNext()){
			ShopType type = typeIterator.next();
			if(!player.hasPermission("shop.create." + type.toString())){
				typeIterator.remove();
			}
		}
		return typeList;
	}
	
	/**
	 * @param player the player to check for
	 * @return how many shops the player can build total (does not include already built shops)
	 */
	public static int getShopBuildLimit(Permissible player) {
		if(player.isOp()){
			return 99999;
		}
		int baseBuildLimit = -1;
		int extraBuildLimit = 0;
		Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
		
		// calculate base buildlimit permission first (highest number)
		for(PermissionAttachmentInfo permInfo : permissions){
			String perm = permInfo.getPermission();
			// Skip if not a shop permission
			if(!perm.startsWith("shop.")){
				continue;
			}
			
			// If it's a base build limit permission, parse the number
			int value = 0;
			try{
				value = Integer.parseInt(perm.substring(perm.lastIndexOf(".") + 1));
			} catch(NumberFormatException e){
				continue;
			}
			if(perm.startsWith("shop.buildlimit.")){
				if(value > baseBuildLimit){
					baseBuildLimit = value;
				}
			}
			
			// If it's an extra build limit permission, parse the number
			else if(perm.startsWith("shop.buildlimitextra.")){
				extraBuildLimit += value;
				
			}
		}
		return baseBuildLimit + extraBuildLimit;
	}
	
	/**
	 * Get all shops this player owns
	 */
	public static List<AbstractShop> getShops(UUID uuid) {
		return Shop.getPlugin().getShopHandler().getShops(uuid);
	}
}
