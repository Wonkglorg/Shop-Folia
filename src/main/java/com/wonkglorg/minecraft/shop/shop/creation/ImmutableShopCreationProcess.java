package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.shop.shop.ShopType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ImmutableShopCreationProcess(Player player, boolean playerIsOperator, UUID playerUUID, UUID shopId, Sign sign, Block container,
                                           BlockFace signDirection, ShopType type, int amount, double price, double priceCombo, boolean adminShop,
                                           boolean isFakeSign, @Nullable ItemStack itemStack, @Nullable ItemStack barterStack,
                                           boolean finishedInitialisation, boolean isCancelled){}