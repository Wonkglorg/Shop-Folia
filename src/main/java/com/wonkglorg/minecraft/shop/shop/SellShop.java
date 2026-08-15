package com.wonkglorg.minecraft.shop.shop;

import com.wonkglorg.minecraft.shop.shop.display.DisplayType;
import com.wonkglorg.minecraft.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class SellShop extends AbstractShop {

    public SellShop(UUID shopId, Location signLoc, UUID player, double pri, int amt, Boolean admin, BlockFace facing, long creationDate,
                    DisplayType type) {
        super(shopId,signLoc, player, pri, amt, admin, facing,creationDate,type);

        this.creationWord = CreationWord.SELL;
        this.type = ShopType.SELL;
        this.signLines = ShopMessage.getSignLines(this);
    }

}
