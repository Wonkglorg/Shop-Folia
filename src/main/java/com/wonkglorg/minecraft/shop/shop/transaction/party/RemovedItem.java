package com.wonkglorg.minecraft.shop.shop.transaction.party;

import org.bukkit.inventory.ItemStack;

/**
 * Represents an item that was removed
 * @param stack the removed item reference
 * @param amount the amount removed
 */
public record RemovedItem(ItemStack stack, int amount){}
