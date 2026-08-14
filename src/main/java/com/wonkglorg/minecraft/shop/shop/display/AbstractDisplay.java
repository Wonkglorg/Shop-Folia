package com.wonkglorg.minecraft.shop.shop.display;

import com.mojang.datafixers.util.Pair;
import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.ArmorStandData;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import io.papermc.paper.adventure.PaperAdventure;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractDisplay{
	
	protected Shop plugin;
	@Getter
	protected DisplayType type;
	@Getter
	protected AbstractShop shop;
	
	protected Map<UUID, List<Integer>> entityIDs; //player UUID. display entities
	
	protected AbstractDisplay(AbstractShop shop, DisplayType type) {
		this.plugin = Shop.getPlugin();
		this.shop = shop;
		this.type = type;
	}
	
	/**
	 * Spawns the display for all players
	 */
	public void spawn() {
		for(var player : Bukkit.getOnlinePlayers()){
			spawn(player);
		}
	}
	
	/**
	 * Spawns the display for the player
	 */
	public abstract void spawn(@NotNull Player player);
	
	/**
	 * Removes the display from all players
	 */
	public void remove() {
		for(var player : Bukkit.getOnlinePlayers()){
			remove(player);
		}
	}
	
	/**
	 * Removes the display from the player
	 */
	public void remove(@NotNull Player player) {
		removeLight();
		List<Integer> entityIds = entityIDs.remove(player.getUniqueId());
		if(entityIds != null){
			for(var entityId : entityIds){
				ClientboundRemoveEntitiesPacket destroyEntityPacket = new ClientboundRemoveEntitiesPacket(entityId);
				sendPacket(player, destroyEntityPacket);
			}
		}
	}
	
	/**
	 * Adds an entity id to be tracked
	 */
	protected void addEntityId(Player player, int entityId) {
		entityIDs.computeIfAbsent(player.getUniqueId(), _ -> new ArrayList<>()).add(entityId);
	}
	
	/**
	 * Sends a packet to the player
	 */
	protected void sendPacket(Player player, Packet<?> packet) {
		try{
			if(isSameWorld(player)){
				((CraftPlayer) player).getHandle().connection.send(packet);
			}
			
		} catch(Exception e){
			Shop.getPlugin().getLogger().severe("Unknown error sending packet to player for Display (Item/Hologram text), error message: " +
			                                    e.getMessage());
		}
	}
	
	public boolean isChunkLoaded() {
		return this.getShop().getSignLocation().isChunkLoaded();
	}
	
	//spawns an armor stand packet for a specific player
	//if player is null, all online players will get the packet
	protected void spawnArmorStandPacket(Player player, ArmorStandData armorStandData, Component text) {
		Location location = armorStandData.getLocation();
		ArmorStand armorStand = new ArmorStand(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ());
		armorStand.setYRot((float) armorStandData.getYaw());
		
		armorStand.setCustomName(PaperAdventure.asVanilla(text));
		armorStand.setCustomNameVisible(true);
		this.addDisplayTag(player, armorStand.getId());
		
		if(armorStandData.getRightArmPose() != null){
			EulerAngle angle = armorStandData.getRightArmPose(); //EulerAngles are in radians
			float x = (float) Math.toDegrees(angle.getX());
			float y = (float) Math.toDegrees(angle.getY());
			float z = (float) Math.toDegrees(angle.getZ());
			armorStand.setRightArmPose(new Rotations(x, y, z));
		}
		armorStand.setMarker(true);
		armorStand.setNoGravity(true);
		armorStand.setInvulnerable(true);
		armorStand.setInvisible(true);
		armorStand.persist = true;
		armorStand.collides = false;
		
		if(armorStandData.isSmall()){
			armorStand.setSmall(true);
		}
		
		Shop.getPlugin().getLogger().log(java.util.logging.Level.FINE, "Floating Tag Label Location: " + location);
		
		ClientboundAddEntityPacket spawnEntityLivingPacket = new ClientboundAddEntityPacket(armorStand.getId(),
				armorStand.getUUID(),
				location.getX(),
				location.getY(),
				location.getZ(),
				armorStand.getXRot(),
				armorStand.getYRot(),
				armorStand.getType(),
				0,
				armorStand.getDeltaMovement(),
				armorStand.getYHeadRot());
		ClientboundSetEntityDataPacket spawnEntityMetadataPacket = new ClientboundSetEntityDataPacket(armorStand.getId(),
				armorStand.getEntityData().packDirty());
		ClientboundSetEquipmentPacket spawnEntityEquipmentPacket = null;
		
		//armor stand only going to have equipment if text is not populated
		if(text == null){
			List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList();
			var itemStack = CraftItemStack.asNMSCopy(armorStandData.getEquipment());
			equipmentList.add(new Pair<>(getMojangEquipmentSlot(armorStandData.getEquipmentSlot()), itemStack));
			
			spawnEntityEquipmentPacket = new ClientboundSetEquipmentPacket(armorStand.getId(), equipmentList);
		}
		
		sendPacket(player, spawnEntityLivingPacket);
		sendPacket(player, spawnEntityMetadataPacket);
		if(spawnEntityEquipmentPacket != null){
			sendPacket(player, spawnEntityEquipmentPacket);
		}
	}
	
	//DISPLAY TAGS
	
	public void showDisplayTags(Player player) {
		if(displayTagsVisible(player) ||
		   !getShop().isInitialized() ||
		   Shop.getPlugin().getDisplayTagOption() == DisplayTagOption.NONE ||
		   getShop().getFacing() == null){
			return;
		}
		
		try{
			ArrayList<String> displayTags = ShopMessage.getDisplayTags(getShop(), getShop().getType());
			
			Location lowerTagLocation = getShop().getContainerLocation().clone().add(0, 1, 0);
			lowerTagLocation = lowerTagLocation.add(0.5, 0.5, 0.5);
			
			//push the tag slightly closer to the front of the shop so it doesnt collide with the display and hide the text
			lowerTagLocation = UtilMethods.pushLocationInDirection(lowerTagLocation, this.getShop().getFacing(), 0.2);
			
			Block displayBlock = lowerTagLocation.getBlock();
			if(this.isChunkLoaded()){
				if(displayBlock.getType() == Material.BARREL || displayBlock.getRelative(BlockFace.DOWN).getType() == Material.BARREL){
					lowerTagLocation = lowerTagLocation.add(0, .25, 0);
				}
				// If there is a block above our display, offset the tag location
				// so that it doesn't become hidden inside the block. (most noticible with chests)
				if(getShop().getContainerLocation().clone().add(0, 2, 0).getBlock().getType() != Material.AIR){
					// Adds 0.35 on top of the 0.2 added above (total of 0.55)
					// 0.3 to get to edge of block, 0.05 to give a lil more wiggle room when the player isnt looking directly at the display
					lowerTagLocation = UtilMethods.pushLocationInDirection(lowerTagLocation, this.getShop().getFacing(), 0.35);
				}
			}
			
			// Create a list to store tag data
			List<Map.Entry<String, Location>> tagData = new ArrayList<>();
			
			double verticalAddition = 0;
			//iterate through list backwards to build from bottom -> up
			for(int i = displayTags.size() - 1; i >= 0; i--){
				Location asTagLocation = lowerTagLocation.clone();
				
				String tagLine = displayTags.get(i);
				if(tagLine.contains("[lshift]")){
					asTagLocation = asTagLocation.add(getShiftOffset(true, false));
					tagLine = tagLine.replace("[lshift]", "");
				}
				if(tagLine.contains("[rshift]")){
					asTagLocation = asTagLocation.add(getShiftOffset(false, true));
					tagLine = tagLine.replace("[rshift]", "");
				}
				
				asTagLocation = asTagLocation.add(0, verticalAddition, 0);
				
				// Store the tag data instead of creating it immediately
				tagData.add(new AbstractMap.SimpleEntry<>(tagLine, asTagLocation));
				
				verticalAddition += 0.3;
			}
			
			// Now create the tags in reverse order (top to bottom)
			for(int i = tagData.size() - 1; i >= 0; i--){
				Map.Entry<String, Location> entry = tagData.get(i);
				String tagLine = entry.getKey();
				Location asTagLocation = entry.getValue();
				
				Shop.getPlugin().logger().spam("[Display] Adding tag line: " + tagLine);
				//todo:jmd implement proper tag line
				createTagEntity(player, Component.text(tagLine), asTagLocation);
			}
			
			//todo:mjd check what this is exactly and what changes if its not present
			//Shop.getPlugin().getShopmanager().getDisplayManager().addActiveShopDisplayTag(player, this.shopSignLocation);
			
			//this handles getting rid of the display tags after a configured amount of time after the player looks away from the shop sign
			removeDisplayTagsDelayedTask(player);
			
		} catch(NullPointerException e){
			e.printStackTrace();
		}
	}
	
	public void updateDisplayTags() {
		// Update any players display tags who currently have them open
		if(displayTagEntityIDs == null || displayTagEntityIDs.isEmpty()){
			return;
		}
		
		// Get a copy of the keys to avoid concurrent modification issues
		Set<UUID> playerUUIDs = new HashSet<>(displayTagEntityIDs.keySet());
		
		for(UUID playerUUID : playerUUIDs){
			Player player = Shop.getPlugin().getServer().getPlayer(playerUUID);
			
			// Skip if player is offline or in a different world
			if(player == null || !player.isOnline() || !isSameWorld(player)){
				continue;
			}
			
			// Check if player has display tags visible
			if(displayTagsVisible(player)){
				// Remove the current display tags
				removeDisplayEntities(player, true);
				
				// Show updated display tags
				showDisplayTags(player);
			}
		}
	}
	
	public void createTagEntity(Player player, Component text, Location location) {
		Shop.getPlugin().logger().debug("Spawning hologram for player " +
		                                player.getName() +
		                                " at " +
		                                location.getBlockX() +
		                                "/" +
		                                location.getBlockY() +
		                                "/" +
		                                location.getBlockZ() +
		                                ": " +
		                                text);
		ArmorStandData caseStandData = new ArmorStandData();
		caseStandData.setSmall(false);
		caseStandData.setLocation(location);
		
		spawnArmorStandPacket(player, caseStandData, text);
	}
	
	public void setType(DisplayType type, boolean checkDisplayBlock) {
		DisplayType oldType = this.type;
		
		if(checkDisplayBlock && getShop().getContainerLocation() != null){
			if((oldType == DisplayType.NONE && type != DisplayType.ITEM_FRAME) || (oldType == DisplayType.ITEM_FRAME && type != DisplayType.NONE)){
				if(this.isChunkLoaded()){
					//make sure there is room above the shop for the display
					Block aboveShop = this.getShop().getContainerLocation().getBlock().getRelative(BlockFace.UP);
					if(!UtilMethods.materialIsNonIntrusive(aboveShop.getType())){
						return;
					}
				}
			}
		}
		
		this.type = type;
	}
	
	public void cycleType(Player player) {
		if(getShop().getFacing() == null){
			return;
		}
		DisplayType[] cycle = Shop.getPlugin().getSettingsConfig().getDisplayCycle();
		DisplayType displayType = this.type;
		if(displayType == null){
			displayType = Shop.getPlugin().getSettingsConfig().getDisplayTypeDefault();
		}
		
		int index = -1;
		if(displayType == DisplayType.NONE){
			//make sure there is room above the shop for the display
			Block aboveShop = this.getShop().getContainerLocation().getBlock().getRelative(BlockFace.UP);
			if(!UtilMethods.materialIsNonIntrusive(aboveShop.getType())){
				//if the cycle contains the ITEM_FRAME display type
				for(int i = 0; i < cycle.length; i++){
					if(cycle[i] == DisplayType.ITEM_FRAME){
						index = i;
					}
				}
				//there is no ITEM_FRAME in cycle, return because display is blocked
				if(index == -1){
					return;
				}
			}
		} else if(displayType == DisplayType.ITEM_FRAME){
			//make sure there is room above the shop for the display
			Block aboveShop = this.getShop().getContainerLocation().getBlock().getRelative(BlockFace.UP);
			if(!UtilMethods.materialIsNonIntrusive(aboveShop.getType())){
				//if the cycle contains the NONE display type
				for(int i = 0; i < cycle.length; i++){
					if(cycle[i] == DisplayType.NONE){
						index = i;
					}
				}
				//there is no NONE in cycle, return because display is blocked
				if(index == -1){
					return;
				}
			}
		}
		
		//index is still not set, continue and cycle index to next display type
		if(index == -1){
			index = 0;
			for(int i = 0; i < cycle.length; i++){
				if(cycle[i] == displayType){
					index = i + 1;
				}
			}
			if(index >= cycle.length){
				index = 0;
			}
		}
		
		//don't allow barter shops to have ITEM_FRAME display types (for NOW)
		if(cycle[index] == DisplayType.ITEM_FRAME){
			
			boolean skip = false;
			if(getShop().getType() == ShopType.BARTER){
				skip = true;
			} else {
				//calculate where ITEM_FRAME display may be
				for(Entity e : this.getShop().getContainerLocation().getWorld().getNearbyEntities(this.getItemDropLocation(false), 1, 1, 1)){
					if(e.getType() == EntityType.ITEM_FRAME){
						ItemFrame i = (ItemFrame) e;
						if(i.getAttachedFace() == getShop().getSign().getFacing().getOppositeFace()){
							skip = true;
							break;
						}
					}
				}
			}
			
			if(skip){
				index++;
				if(index >= cycle.length){
					index = 0;
				}
			}
		}
		
		this.setType(cycle[index], true);
		this.spawn(player);
		//Shop.getPlugin().getShopmanager().addActiveShopDisplay(player, this.shopSignLocation);
		getShop().setNeedsSave(true);
	}
	
	/**
	 * Gets the primary location of the display
	 */
	protected Location getPrimaryLocation() {
		return getItemDropLocation(false);
	}
	
	/**
	 * Gets the secondary location of the display (used when the shop is a barter shop)
	 */
	protected Location getBarterLocation() {
		return getItemDropLocation(true);
	}
	
	/**
	 * Spawns a light above the shop if enabled in the config
	 */
	protected void spawnLight() {
		if(plugin.getSettingsConfig().getDisplayLightLevel() == 0){
			return;
		}
		
		Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
		if(displayBlock.getType() == Material.AIR){
			displayBlock.setType(Material.LIGHT);
			Light data = (Light) displayBlock.getBlockData();
			data.setLevel(Shop.getPlugin().getSettingsConfig().getDisplayLightLevel());
			displayBlock.setBlockData(data);
		}
	}
	
	/**
	 * Removes a spawned light
	 */
	protected void removeLight() {
		Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
		if(displayBlock.getType() == Material.LIGHT){
			displayBlock.setType(Material.AIR);
		}
	}
	
	private Location getItemDropLocation(boolean isBarterItem) {
		if(shop == null || shop.getFacing() == null){
			return null;
		}
		
		//calculate which x,z to drop items at depending on direction of the shop sign
		double dropY = 0.98; // 1 - 0.02 to account for dropped item shadow
		Material blockType = shop.getContainerLocation().getBlock().getType();
		if(blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST){
			dropY = 0.9;
		}
		double dropX = 0.5;
		double dropZ = 0.5;
		if(shop.getType() == ShopType.BARTER){
			switch(shop.getFacing()) {
				case NORTH:
					if(isBarterItem){
						dropX = 0.3;
					} else {
						dropX = 0.7;
					}
					break;
				case EAST:
					if(isBarterItem){
						dropZ = 0.3;
					} else {
						dropZ = 0.7;
					}
					break;
				case SOUTH:
					if(isBarterItem){
						dropX = 0.7;
					} else {
						dropX = 0.3;
					}
					break;
				case WEST:
					if(isBarterItem){
						dropZ = 0.7;
					} else {
						dropZ = 0.3;
					}
					break;
				default:
					dropX = 0.5;
					dropZ = 0.5;
					break;
			}
		}
		return shop.getContainerLocation().clone().add(dropX, dropY, dropZ);
	}
	
	public Vector getShiftOffset(boolean isLeftShift, boolean isRightShift) {
		AbstractShop shop = this.getShop();
		
		Vector offset = new Vector(0, 0, 0);
		double space = 0.48;
		
		switch(shop.getFacing()) {
			case NORTH:
				if(isRightShift){
					offset.setX(-space);
				} else if(isLeftShift){
					offset.setX(space);
				}
				break;
			case EAST:
				if(isRightShift){
					offset.setZ(-space);
				} else if(isLeftShift){
					offset.setZ(space);
				}
				break;
			case SOUTH:
				if(isRightShift){
					offset.setX(space);
				} else if(isLeftShift){
					offset.setX(-space);
				}
				break;
			case WEST:
				if(isRightShift){
					offset.setZ(space);
				} else if(isLeftShift){
					offset.setZ(-space);
				}
				break;
		}
		return offset;
	}
	
	protected boolean playerIsLookingTowardShop(Player player) {
		try{
			if(player.getLocation().distanceSquared(this.shopSignLocation) > 64){ //player is more than 8 blocks away
				return false;
			}
		} catch(IllegalArgumentException _){
			return false;
		}
		Vector lookDirection = player.getEyeLocation().getDirection();
		Location displayLocation = this.getItemDropLocation(false);
		if(displayLocation == null){
			return false;
		}
		Vector blockDirection = displayLocation.subtract(player.getEyeLocation()).toVector().normalize();
		double angle = blockDirection.angle(lookDirection);
		//return true if angle (in radians) is less than 1
		return angle < 1;
	}
	
	protected void removeDisplayTagsDelayedTask(Player player) {
		//remove all armor stand name tag entities after x seconds
		Shop.getPlugin().getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
			if(!displayTagsVisible(player)){
				removeDisplayEntities(player, true);
				return;
			}
			if(playerIsLookingTowardShop(player)){
				removeDisplayTagsDelayedTask(player);
			} else {
				removeDisplayEntities(player, true);
			}
		}, 20);
	}
	
	protected boolean isSameWorld(Player player) {
		return player.getWorld().getUID().equals(this.shop.getSignLocation().getWorld().getUID());
	}
	
	/**
	 * Checks if a chunk is loaded
	 *
	 * @param location The location to check
	 * @return True if the chunk is loaded, false otherwise
	 *
	 * Note: This method should be used instead of `location.getChunk().isChunkLoaded()`
	 * because calling `location.getChunk()` will force a chunk load, which defeats
	 * the purpose of checking if the chunk is already loaded.
	 */
	protected boolean isChunkLoaded(Location location) {
		if(location == null || location.getWorld() == null){
			return false;
		}
		return location.getWorld().isChunkLoaded(UtilMethods.floor(location.getBlockX()) >> 4, UtilMethods.floor(location.getBlockZ()) >> 4);
	}
}