package com.wonkglorg.minecraft.shop.shop.display;

import com.mojang.datafixers.util.Pair;
import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.shop.ShopType;
import com.wonkglorg.minecraft.shop.util.ArmorStandData;
import io.papermc.paper.adventure.PaperAdventure;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractDisplay{
	
	protected Main plugin;
	@Getter
	protected DisplayType type;
	@Getter
	protected AbstractShop shop;
	
	protected Map<UUID, List<Integer>> entityIDs; //player UUID. display entities
	
	protected AbstractDisplay(AbstractShop shop, DisplayType type) {
		this.plugin = Main.getPlugin();
		this.shop = shop;
		this.type = type;
	}
	
	public static AbstractDisplay createDisplay(DisplayType type, AbstractShop shop) {
		return switch(type) {
			case NONE -> new NonDisplay(shop);
			case ITEM -> new ItemDisplay(shop);
			case LARGE_ITEM -> new LargeItemDisplay(shop);
			case GLASS_CASE -> new GlassCaseDisplay(shop);
			case ITEM_FRAME -> new ItemFrameDisplay(shop);
		};
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
	public void spawn(@NotNull Player player) {
		remove();//if anything exists already remove the old stuff first
		spawnLight();
		onSpawn(player);
	}
	
	/**
	 * When display spawning is called
	 */
	protected abstract void onSpawn(Player player);
	
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
	
	protected ClientboundAddEntityPacket createEntity(Player player, net.minecraft.world.entity.Entity entity, int data) {
		entityIDs.computeIfAbsent(player.getUniqueId(), _ -> new ArrayList<>()).add(entity.getId());
		return new ClientboundAddEntityPacket(entity.getId(),
				entity.getUUID(),
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				entity.getXRot(),
				entity.getYRot(),
				entity.getType(),
				data,
				entity.getDeltaMovement(),
				entity.getYHeadRot());
	}
	
	protected ClientboundAddEntityPacket createEntity(Player player, net.minecraft.world.entity.Entity entity, Location location, int data) {
		entityIDs.computeIfAbsent(player.getUniqueId(), _ -> new ArrayList<>()).add(entity.getId());
		return new ClientboundAddEntityPacket(entity.getId(),
				entity.getUUID(),
				location.getX(),
				location.getY(),
				location.getZ(),
				entity.getXRot(),
				entity.getYRot(),
				entity.getType(),
				data,
				entity.getDeltaMovement(),
				entity.getYHeadRot());
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
			Main.getPlugin().getLogger().severe("Unknown error sending packet to player for Display (Item/Hologram text), error message: " +
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
		
		Main.getPlugin().getLogger().log(java.util.logging.Level.FINE, "Floating Tag Label Location: " + location);
		
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
	
	public void setType(DisplayType type, boolean checkDisplayBlock) {
		DisplayType oldType = this.type;
		
		if(checkDisplayBlock && getShop().getContainerLocation() != null){
			if((oldType == DisplayType.NONE && type != DisplayType.ITEM_FRAME) || (oldType == DisplayType.ITEM_FRAME && type != DisplayType.NONE)){
				if(this.isChunkLoaded()){
					//make sure there is room above the shop for the display
					Block aboveShop = this.getShop().getContainerLocation().getBlock().getRelative(BlockFace.UP);
					if(!(aboveShop.getType() == Material.AIR)) {
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
		DisplayType[] cycle = Main.getPlugin().getSettingsConfig().getDisplayCycle();
		DisplayType displayType = this.type;
		if(displayType == null){
			displayType = Main.getPlugin().getSettingsConfig().getDisplayTypeDefault();
		}
		
		int index = -1;
		if(displayType == DisplayType.NONE){
			//make sure there is room above the shop for the display
			Block aboveShop = this.getShop().getContainerLocation().getBlock().getRelative(BlockFace.UP);
			if(aboveShop.getType() == Material.AIR){
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
			if(aboveShop.getType() == Material.AIR){
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
				for(Entity e : this.getShop().getContainerLocation().getWorld().getNearbyEntities(this.getBarterLocation(), 1, 1, 1)){
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
	
	private EquipmentSlot getMojangEquipmentSlot(org.bukkit.inventory.EquipmentSlot equipmentSlot) {
		switch(equipmentSlot) {
			case HAND:
				return EquipmentSlot.MAINHAND;
			case OFF_HAND:
				return EquipmentSlot.OFFHAND;
			case FEET:
				return EquipmentSlot.FEET;
			case LEGS:
				return EquipmentSlot.LEGS;
			case CHEST:
				return EquipmentSlot.CHEST;
			default:
				return EquipmentSlot.HEAD;
		}
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
	private void spawnLight() {
		if(plugin.getSettingsConfig().getDisplayLightLevel() == 0){
			return;
		}
		
		Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
		if(displayBlock.getType() == Material.AIR){
			displayBlock.setType(Material.LIGHT);
			Light data = (Light) displayBlock.getBlockData();
			data.setLevel(Main.getPlugin().getSettingsConfig().getDisplayLightLevel());
			displayBlock.setBlockData(data);
		}
	}
	
	/**
	 * Removes a spawned light
	 */
	private void removeLight() {
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
	
	protected boolean isSameWorld(Player player) {
		return player.getWorld().getUID().equals(this.shop.getSignLocation().getWorld().getUID());
	}
	
	/**
	 * spawns a floating item packet for a specific player
	 */
	protected void spawnItemPacket(Player player, ItemStack is, Location location) {
		net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(is);
		Level serverLevel = ((CraftWorld) location.getWorld()).getHandle();
		
		ItemEntity entityItem = new ItemEntity(serverLevel, location.getX(), location.getY(), location.getZ(), itemStack);
		int entityID = entityItem.getId();
		entityItem.setInvulnerable(true);
		entityItem.setRemainingFireTicks(-1);
		entityItem.setNoGravity(true);
		entityItem.persist = true;
		entityItem.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.0D)); //not sure if this is the same as setMot() that was there first
		entityItem.setPickUpDelay(32767);
		entityItem.setTicksFrozen(2147483647);
		
		ClientboundAddEntityPacket entitySpawnPacket = createEntity(player, entityItem, location, 0);
		ClientboundSetEntityMotionPacket entityVelocityPacket = new ClientboundSetEntityMotionPacket(entityItem);
		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, entityItem.getEntityData().packDirty());
		
		sendPacket(player, entitySpawnPacket);
		sendPacket(player, entityVelocityPacket);
		sendPacket(player, entityMetadataPacket);
	}
}