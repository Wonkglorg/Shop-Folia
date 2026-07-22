package com.snowgears.shop.shop;

import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class SellShop extends AbstractShop {

    public SellShop(Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing) {
        super(signLoc, player, pri, amt, admin, facing);

        this.type = ShopType.SELL;
        this.signLines = ShopMessage.getSignLines(this);
    }

}
