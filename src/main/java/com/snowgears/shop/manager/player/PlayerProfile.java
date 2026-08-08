package com.snowgears.shop.manager.player;

import com.snowgears.shop.Constants;
import com.snowgears.shop.Shop;
import static com.snowgears.shop.manager.PlayerManager.loadfromFile;
import static com.snowgears.shop.manager.PlayerManager.saveToFile;
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
	 * Toggles user notifications
	 *
	 * @return the value it toggled to
	 */
	public boolean toggleNotifyUser() {
		notifyUser = !notifyUser;
		saveToFile(this);
		return notifyUser;
	}
	
	/**
	 * Toggles owner notifications
	 *
	 * @return the value it toggled to
	 */
	public boolean toggleNotifyOwner() {
		notifyOwner = !notifyOwner;
		saveToFile(this);
		return notifyOwner;
	}
	
	/**
	 * Toggles stock notifications
	 *
	 * @return the value it toggled to
	 */
	public boolean toggleNotifyStock() {
		notifyStock = !notifyStock;
		saveToFile(this);
		return notifyStock;
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
	public static boolean isAllowedToCreateShop(Permissible player, ShopType type) {
		return hasActionPermission("shop.create", player, type);
	}
	
	/**
	 * If the user is allowed to create a shop of any type, to find out what specific type they can create use {@link #isAllowedToCreateShop(Permissible, ShopType)} instead
	 */
	public static boolean isAllowedToCreateShop(Permissible player) {
		return hasActionPermission("shop.create", player);
	}
	
	/**
	 * If the user is allowed to use any of the shop types, to find out what specific type they can use, use {@link #isAllowedToUseShop(Permissible, ShopType)} instead
	 */
	public static boolean isAllowedToUseShop(Permissible player) {
		return hasActionPermission("shop.use", player);
	}
	
	/**
	 * If the user is allowed to use a shop of this type
	 */
	public static boolean isAllowedToUseShop(Permissible player, ShopType type) {
		return hasActionPermission("shop.use", player, type);
	}
	
	/**
	 * If the user is allowed to destroy any of the shop types, to find out what specific type they can destroy, use {@link #isAllowedToDestroyShop(Permissible, ShopType)} instead
	 */
	public static boolean isAllowedToDestroyShop(Permissible player) {
		return hasActionPermission("shop.destroy", player);
	}
	
	/**
	 * If the user is allowed to use a shop of this type
	 */
	public static boolean isAllowedToDestroyShop(Permissible player, ShopType type) {
		return hasActionPermission("shop.destroy", player, type);
	}
	
	/**
	 * If the user is allowed to destroy someone else's shop
	 */
	public static boolean isAllowedToDestroyShopOther(Permissible player) {
		return isOperator(player) || player.hasPermission("shop.destroy.other");
	}
	
	private static boolean hasActionPermission(String permissionBase, Permissible player) {
		if(isOperator(player)){
			return true;
		}
		if(player.hasPermission(permissionBase)){
			return true;
		}
		
		for(ShopType shopType : ShopType.values()){
			if(player.hasPermission(permissionBase + "." + shopType.toString().toLowerCase())){
				return true;
			}
		}
		return false;
	}
	
	private static boolean hasActionPermission(String permissionBase, Permissible player, ShopType type) {
		if(isOperator(player)){
			return true;
		}
		if(player.hasPermission(permissionBase)){
			return true;
		}
		
		return player.hasPermission(permissionBase + "." + type.toString().toLowerCase());
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
		
		typeList.removeIf(type -> !player.hasPermission("shop.create." + type.toString()));
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
