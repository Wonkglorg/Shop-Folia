package com.snowgears.shop.display;

import com.mojang.datafixers.util.Pair;
import com.snowgears.shop.Shop;
import com.snowgears.shop.util.ArmorStandData;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Display extends AbstractDisplay{
	
	public Display(Location shopSignLocation) {
		super(shopSignLocation);
	}
	
	@Override
	protected void spawnItemPacket(Player player, ItemStack is, Location location) {
		net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(is);
		Level serverLevel = ((CraftWorld) location.getWorld()).getHandle();
		
		ItemEntity entityItem = new ItemEntity(serverLevel, location.getX(), location.getY(), location.getZ(), itemStack);
		int entityID = entityItem.getId();
		this.addEntityID(player, entityID);
		entityItem.setInvulnerable(true);
		entityItem.setRemainingFireTicks(-1);
		entityItem.setNoGravity(true);
		entityItem.persist = true;
		entityItem.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.0D)); //not sure if this is the same as setMot() that was there first
		entityItem.setPickUpDelay(32767);
		entityItem.setTicksFrozen(2147483647);
		
		Shop.getPlugin().getLogger().log(java.util.logging.Level.FINE, "Item Location: " + location);
		
		ClientboundRemoveEntitiesPacket entityDestroyPacket = new ClientboundRemoveEntitiesPacket(entityID);
		ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(entityItem.getId(),
				entityItem.getUUID(),
				location.getX(),
				location.getY(),
				location.getZ(),
				entityItem.getXRot(),
				entityItem.getYRot(),
				entityItem.getType(),
				0,
				entityItem.getDeltaMovement(),
				entityItem.getYHeadRot());
		ClientboundSetEntityMotionPacket entityVelocityPacket = new ClientboundSetEntityMotionPacket(entityItem);
		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, entityItem.getEntityData().packDirty());
		
		sendPacket(player, entityDestroyPacket);
		sendPacket(player, entitySpawnPacket);
		sendPacket(player, entityVelocityPacket);
		sendPacket(player, entityMetadataPacket);
	}
	
	@Override
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
			List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = new ArrayList();
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
	
	@Override
	protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing) {
		ServerLevel worldServer = ((CraftWorld) location.getWorld()).getHandle();
		BlockPos blockPosition = new BlockPos((int) location.getX(), (int) location.getY(), (int) location.getZ());
		ItemFrame itemFrame;
		
		if(isGlowing){
			itemFrame = new GlowItemFrame(worldServer, blockPosition, getMojangDirection(facing));
		} else {
			itemFrame = new ItemFrame(worldServer, blockPosition, getMojangDirection(facing));
		}
		
		int entityID = itemFrame.getId();
		this.addEntityID(player, entityID);
		itemFrame.setPos(location.getX(), location.getY(), location.getZ());
		var itemStack = CraftItemStack.asNMSCopy(is);
		
		itemFrame.setItem(itemStack);
		itemFrame.setDirection(getMojangDirection(facing));
		
		Shop.getPlugin().getLogger().log(java.util.logging.Level.FINE, "ItemFrame Location: " + location);
		
		ClientboundAddEntityPacket entitySpawnPacket = new ClientboundAddEntityPacket(itemFrame.getId(),
				itemFrame.getUUID(),
				location.getX(),
				location.getY(),
				location.getZ(),
				itemFrame.getXRot(),
				itemFrame.getYRot(),
				itemFrame.getType(),
				itemFrame.getDirection().get3DDataValue(),
				itemFrame.getDeltaMovement(),
				itemFrame.getYHeadRot());
		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, itemFrame.getEntityData().packDirty());
		
		sendPacket(player, entitySpawnPacket);
		sendPacket(player, entityMetadataPacket);
	}
	
	private void sendPacket(Player player, Packet packet) {
		try{
			if(isSameWorld(player)){
				((CraftPlayer) player).getHandle().connection.send(packet);
			}
			
		} catch(Exception e){
			Shop.getPlugin().getLogger().severe("Unknown error sending packet to player for Display (Item/Hologram text), error message: " +
			                                    e.getMessage());
		}
	}
	
	@Override
	public void removeDisplayEntities(Player player, boolean onlyDisplayTags) {
		Iterator<Integer> entityIterator = this.getDisplayEntityIDIterator(player, onlyDisplayTags);
		if(entityIterator == null){
			return;
		}
		
		while(entityIterator.hasNext()){
			int displayEntityID = entityIterator.next();
			ClientboundRemoveEntitiesPacket destroyEntityPacket;
			try{
				destroyEntityPacket = new ClientboundRemoveEntitiesPacket(displayEntityID);
			} catch(NoSuchMethodError e){
				throw new RuntimeException(e);
			}
			sendPacket(player, destroyEntityPacket);
			entityIterator.remove();
		}
		if(onlyDisplayTags){
			if(player != null && displayTagEntityIDs != null){
				displayTagEntityIDs.remove(player.getUniqueId());
			}
		}
	}
	
	private Direction getMojangDirection(BlockFace facing) {
		switch(facing) {
			case NORTH:
				return Direction.NORTH;
			case SOUTH:
				return Direction.SOUTH;
			case WEST:
				return Direction.WEST;
			case EAST:
				return Direction.EAST;
			case DOWN:
				return Direction.DOWN;
			default:
				return Direction.UP;
		}
	}
	
	private net.minecraft.world.entity.EquipmentSlot getMojangEquipmentSlot(EquipmentSlot equipmentSlot) {
		switch(equipmentSlot) {
			case HAND:
				return net.minecraft.world.entity.EquipmentSlot.MAINHAND;
			case OFF_HAND:
				return net.minecraft.world.entity.EquipmentSlot.OFFHAND;
			case FEET:
				return net.minecraft.world.entity.EquipmentSlot.FEET;
			case LEGS:
				return net.minecraft.world.entity.EquipmentSlot.LEGS;
			case CHEST:
				return net.minecraft.world.entity.EquipmentSlot.CHEST;
			default:
				return net.minecraft.world.entity.EquipmentSlot.HEAD;
		}
	}
	
	@Override
	public String getItemNameNMS(ItemStack item) {
		var itemStack = CraftItemStack.asNMSCopy(item);
		return itemStack.getItem().getName(itemStack).getString();
	}
}
