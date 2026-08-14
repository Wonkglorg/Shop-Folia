package com.wonkglorg.minecraft.shop.shop.display;

import com.wonkglorg.minecraft.shop.Shop;
import com.wonkglorg.minecraft.shop.shop.AbstractShop;
import com.wonkglorg.minecraft.shop.util.UtilMethods;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ItemDisplay extends AbstractDisplay{
	public ItemDisplay(AbstractShop shop) {
		super(shop, DisplayType.ITEM);
	}
	
	@Override
	public void spawn(@NonNull Player player) {
		ItemStack stack = shop.getItemStack().clone();
		stack.setAmount(1);
		spawnItemPacket(player, stack, this.getPrimaryLocation());
		spawnLight();
		
		if(shop.getSecondaryItemStack() != null){
			ItemStack secondStack = shop.getSecondaryItemStack().clone();
			secondStack.setAmount(1);
			spawnItemPacket(player, secondStack, this.getBarterLocation());
		}
	}
	
	//spawns a floating item packet for a specific player
	//if player is null, all online players will get the packet
	protected void spawnItemPacket(@Nullable Player player, ItemStack is, Location location) {
		//also spawn a light if need be. its related to the display afterall
		if(plugin.getSettingsConfig().getDisplayLightLevel() > 0){
			Block displayBlock = shop.getContainerLocation().getBlock().getRelative(BlockFace.UP);
			if(UtilMethods.materialIsNonIntrusive(displayBlock.getType())){
				displayBlock.setType(Material.LIGHT);
				Light data = (Light) displayBlock.getBlockData();
				data.setLevel(Shop.getPlugin().getSettingsConfig().getDisplayLightLevel());
				displayBlock.setBlockData(data);
			}
		}
		
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
	addEntityID(player,entityID);
	}
}
