package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.ShopPlugin;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;

public class ItemFrameDisplay extends AbstractDisplay{
	protected ItemFrameDisplay(AbstractShop shop) {
		super(shop, DisplayType.ITEM_FRAME);
	}
	
	@Override
	public List<Integer> spawn(@NotNull Player player) {
		Location validLocation = shop.getAboveContainer();
		if(validLocation.getBlock().getType() != Material.AIR){
			validLocation = shop.getAboveSign();
		}
		List<Integer> entityIds = new ArrayList<>();
		
		entityIds.add(spawnItemFramePacket(player,
				shop.getDisplayItem(),
				validLocation,
				shop.getFacing(),
				ShopPlugin.getPlugin().getSettingsConfig().isDisplayGlowingItemFrame()));
		
		//only add secondary item for item frames if it is a double chest shop
		if(shop.getSecondaryContainerLocation() == null){
			return entityIds;
		}
		
		ItemStack secondaryStack = shop.getSecondaryDisplayItem();
		if(secondaryStack != null){
			Location secondaryValidLocation = shop.getAboveSecondaryContainer();
			assert secondaryValidLocation != null;
			if(secondaryValidLocation.getBlock().getType() != Material.AIR){
				Block relative = shop.getAboveSecondaryContainer().getBlock().getRelative(shop.getFacing());
				if(relative.getType() != Material.AIR){
					return entityIds;
				}
				secondaryValidLocation = relative.getLocation();
			}
			
			entityIds.add(spawnItemFramePacket(player,
					secondaryStack,
					secondaryValidLocation,
					shop.getFacing(),
					ShopPlugin.getPlugin().getSettingsConfig().isDisplayGlowingItemFrame()));
		}
		return entityIds;
	}
	
	//spawns an item frame packet for a specific player
	//if player is null, all online players will get the packet
	protected int spawnItemFramePacket(Player player, ItemStack is, Location location, BlockFace facing, boolean isGlowing) {
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
		
		var entitySpawnPacket = createEntity(itemFrame, itemFrame.getDirection().get3DDataValue());
		var entityMetadataPacket = new ClientboundSetEntityDataPacket(entityID, requireNonNull(itemFrame.getEntityData().packDirty()));
		
		sendPacket(player, entitySpawnPacket);
		sendPacket(player, entityMetadataPacket);
		return entityID;
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
