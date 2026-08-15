package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.Main;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemFrameDisplay extends AbstractDisplay{
	protected ItemFrameDisplay(AbstractShop shop) {
		super(shop, DisplayType.ITEM);
	}
	
	@Override
	public void onSpawn(@NotNull Player player) {
		Location frameLocation;
		//only calculate the item frame location if the shop is in a loaded chunk (because Block is used)
		if(this.isChunkLoaded()){
			Block aboveShop = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
			frameLocation = aboveShop.getLocation();
			//if display is blocked, put item frame on front
			if((aboveShop.getType() == Material.AIR)){
				frameLocation = aboveShop.getRelative(shop.getFacing()).getLocation();
			}
		} else {
			frameLocation = shop.getContainerLocation().clone().add(0, 1, 0);
		}
		
		spawnItemFramePacket(player,
				shop.getItemStack(),
				frameLocation,
				shop.getFacing(),
				Main.getPlugin().getSettingsConfig().isSetGlowingItemFrame());
	}
	
	//spawns an item frame packet for a specific player
	//if player is null, all online players will get the packet
	protected void spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing) {
		ServerLevel worldServer = ((CraftWorld) location.getWorld()).getHandle();
		BlockPos blockPosition = new BlockPos((int) location.getX(), (int) location.getY(), (int) location.getZ());
		net.minecraft.world.entity.decoration.ItemFrame itemFrame;
		
		if(isGlowing){
			itemFrame = new GlowItemFrame(worldServer, blockPosition, getMojangDirection(facing));
		} else {
			itemFrame = new net.minecraft.world.entity.decoration.ItemFrame(worldServer, blockPosition, getMojangDirection(facing));
		}
		
		int entityID = itemFrame.getId();
		itemFrame.setPos(location.getX(), location.getY(), location.getZ());
		var itemStack = CraftItemStack.asNMSCopy(is);
		
		itemFrame.setItem(itemStack);
		itemFrame.setDirection(getMojangDirection(facing));
		
		ClientboundAddEntityPacket entitySpawnPacket = createEntity(player, itemFrame, itemFrame.getDirection().get3DDataValue());
		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, itemFrame.getEntityData().packDirty());
		
		sendPacket(player, entitySpawnPacket);
		sendPacket(player, entityMetadataPacket);
	}
	
	private Direction getMojangDirection(BlockFace facing) {
		return switch(facing) {
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
			case EAST -> Direction.EAST;
			case DOWN -> Direction.DOWN;
			default -> Direction.UP;
		};
	}
}
