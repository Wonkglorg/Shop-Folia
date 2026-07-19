package com.snowgears.shop.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TransactionParty {
    // Party - The player that is a party in the transaction, could be the shop owner, or the player clicking the sign
    private OfflinePlayer party;

    // Inventory - the party inventory to access for currency/items
    private Inventory inventory;

    // The amount the party has to spend, could be vault currency, or from an item currency in their inventory
    private double availableFunds;

    // The item being used for currency in a trade (i.e. barter shop)
    private ItemStack currencyItem;

    // Are we the player who created the transaction (used for error handling)
    private boolean isPlayer;

    // Are we an admin shop
    private boolean isAdmin;

    public TransactionParty(boolean isPlayer, boolean isAdmin, OfflinePlayer party, Inventory inventory) {
        this.isPlayer = isPlayer;
        this.isAdmin = isAdmin;
        this.party = party;
        this.inventory = inventory;
    }

    // Allow creating a party that uses an item for it's currency/available funds.
    public TransactionParty(boolean isPlayer, boolean isAdmin, OfflinePlayer party, Inventory inventory, ItemStack currencyItem) {
        this.isPlayer = isPlayer;
        this.isAdmin = isAdmin;
        this.party = party;
        this.inventory = inventory;
        this.currencyItem = currencyItem;
    }

    public boolean isPlayer() { return this.isPlayer; }

    public int getInventoryQuantity(ItemStack item) {
        // If we are an admin, don't check the shop for inventory
        if (this.isAdmin) { return Integer.MAX_VALUE; }

        return InventoryUtils.getAmount(this.inventory, item);
    }

    // Update the amount of currency the player has available (vault/currency item) and return it.
    public double getAvailableFunds() {
        // If we are an admin, don't check for funds
        if (this.isAdmin) { return Double.MAX_VALUE; }

        // Check if we are using an item for our funds, this will happen if we are a seller in the transaction and we are selling an item
        if (this.currencyItem != null) {
            // We are using an item for our currency, so use that amount!
            this.availableFunds = InventoryUtils.getAmount(this.inventory, this.currencyItem);
        } else {
            // We are using the regular Shop currency for this transaction
            this.availableFunds = EconomyUtils.getFunds(party, this.inventory);
        }

        return this.availableFunds;
    }

    // Check if we have enough space to receive a payment, we might not have the inventory space for it!
    public boolean canAcceptPayment(double paymentAmount) {
        // If we are an admin, then we can accept the payment no matter what
        if (this.isAdmin) { return true; }

        if (this.currencyItem != null) {
            // We are being paid with an item
            ItemStack payment = this.currencyItem.clone();
            payment.setAmount((int) paymentAmount);
            return InventoryUtils.hasRoom(this.inventory, payment);
        }

        // We are being paid through the normal economy
        return EconomyUtils.canAcceptFunds(party, this.inventory, paymentAmount);
    }

    // Receive a payment and add it to the players wallet/inventory
    public void depositFunds(double paymentAmount) {
        // If we are an admin, then we don't deposit any funds
        if (this.isAdmin) { return; }

        if (this.currencyItem != null) {
            // We are being paid with an item
            ItemStack payment = this.currencyItem.clone();
            payment.setAmount((int) paymentAmount);
            InventoryUtils.addItem(this.inventory, payment);
        } else {
            // We are being paid using our normal currency
            EconomyUtils.addFunds(party, this.inventory, paymentAmount);
        }
    }

    // Make a payment for a purchase
    public boolean deductFunds(double paymentAmount) {
        // If we are an admin, then we don't deduct any funds
        if (this.isAdmin) { return true; }

        // Check if we have enough funds to make the payment
        if (this.getAvailableFunds() < paymentAmount) { return false; }

        // Check if we are being paid using an item instead of currency
        if (this.currencyItem != null) {
            ItemStack payment = this.currencyItem.clone();
            payment.setAmount((int) paymentAmount);
            InventoryUtils.removeItem(inventory, payment);
            return true;
        }

        // We are being paid using our normal currency
        return EconomyUtils.removeFunds(party, this.inventory, paymentAmount);
    }

    // Check if there is space in the inventory to recieve an item
    public boolean hasRoomForItem(ItemStack item){
        // If we are an admin, then we always have room for the item (since we don't deposit it)
        if (this.isAdmin) { return true; }

        return InventoryUtils.hasRoom(this.inventory, item);
    }

    public boolean depositItem(ItemStack item) {
        // If we are an admin, then we always have room for the item (since we don't deposit it)
        if (this.isAdmin) { return true; }

        // Check if we have room for the item in our inventory
        if (!this.hasRoomForItem(item)) { return false; }

        // We have the space, so add the item to our inventory!
        // @TODO: Maybe check how many items were unable to be added to the inv to make sure we actually deposited the item
        InventoryUtils.addItem(inventory, item);
        return true;
    }

    public boolean deductItem(ItemStack item) {
        // If we are an admin, then we don't remove the item
        if (this.isAdmin) { return true; }

        // @TODO: Maybe check how many items were unable to be removed from the inv to verify tx occured successfully
        InventoryUtils.removeItem(inventory, item);
        return true;
    }

    @Override
    public String toString() {
        return "TransactionParty{" +
                "isPlayer=" + isPlayer +
                ", funds=" + availableFunds +
                '}';
    }
}
