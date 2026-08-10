package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import com.snowgears.shop.shop.display.AbstractDisplay;
import static com.snowgears.shop.manager.player.PlayerProfile.isAllowedToCreateShop;
import com.snowgears.shop.shop.ShopType;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopCreationProcessOld{
	
	private static final Shop plugin = Shop.getPlugin();
	@Setter
	@Getter
	private ChatCreationStep step;
	@Getter
	private Player player;
	@Getter
	private final UUID playerUUID;
	@Getter
	private Sign sign;
	@Getter
	private Block container;
	private BlockFace signDirection;
	@Getter
	private ItemStack itemStack;
	@Getter
	private ItemStack barterItemStack;
	@Getter
	private ShopType shopType;
	@Setter
	@Getter
	boolean isAdmin;
	
	
	@Getter
	private int amount;
	private PricePair pricePair;
	
	public AbstractDisplay display;
	
	public ShopCreationProcessOld(Player player, Sign sign, Block container, BlockFace signDirection) {
		this.player = player;
		this.playerUUID = player.getUniqueId();
		this.sign = sign;
		this.container = container;
		this.signDirection = signDirection;
		this.step = ChatCreationStep.SIGN_CREATION;
		
		// Displays instructions on top of the chest
		this.display = Shop.getPlugin().getShopmanager().getDisplayManager().createDisplay(container.getLocation());
	}
	

	

	
	public void cleanup() {
		if(this.display.isEnabled()){
			this.display.removeDisplayEntities(player, true);
		}
	}
	
	public void setShopType(ShopType shopType) {
		this.shopType = shopType;
		if(shopType == ShopType.GAMBLE){
			this.step = ChatCreationStep.ITEM_PRICE;
		} else {
			this.step = ChatCreationStep.ITEM_AMOUNT;
		}
	}
	
	public int getItemAmount() {
		if(itemStack == null){
			return 0;
		}
		return itemStack.getAmount();
	}
	
	public void setItemAmount(int itemAmount) {
		this.itemStack.setAmount(itemAmount);
		if(this.shopType == ShopType.BARTER){
			this.step = ChatCreationStep.BARTER_ITEM;
		} else {
			this.step = ChatCreationStep.ITEM_PRICE;
		}
	}
	
	public int getBarterItemAmount() {
		if(barterItemStack == null){
			return 0;
		}
		return barterItemStack.getAmount();
	}
	
	public void setBarterItemAmount(int barterItemAmount) {
		this.barterItemStack.setAmount(barterItemAmount);
		this.step = ChatCreationStep.FINISHED;
	}
	
	public PricePair getPricePair() {
		if(pricePair == null){
			this.pricePair = new PricePair(0, 0);
		}
		return pricePair;
	}
	
	public void setPricePair(PricePair pricePair) {
		this.pricePair = pricePair;
		if(this.shopType == ShopType.COMBO){
			this.step = ChatCreationStep.ITEM_PRICE_COMBO;
		} else {
			this.step = ChatCreationStep.FINISHED;
		}
	}
	
	public UUID getUniqueID() {
		return processUUID;
	}
	
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack.clone();
		this.step = ChatCreationStep.SHOP_TYPE;
	}
	
	public void setBarterItemStack(ItemStack barterItemStack) {
		this.barterItemStack = barterItemStack.clone();
		this.barterItemStack.setAmount(1);
		this.step = ChatCreationStep.BARTER_ITEM_AMOUNT;
	}
	
	public void setPrice(double price) {
		if(pricePair == null){
			pricePair = new PricePair(price, 0);
		}
		pricePair.setPrice(price);
		if(this.shopType == ShopType.COMBO){
			this.step = ChatCreationStep.ITEM_PRICE_COMBO;
		} else {
			this.step = ChatCreationStep.FINISHED;
		}
	}
	
	public void setPriceCombo(double priceCombo) {
		if(pricePair == null){
			pricePair = new PricePair(0, priceCombo);
		}
		pricePair.setPriceCombo(priceCombo);
		this.step = ChatCreationStep.FINISHED;
	}
	
	public void displayFloatingText(String subkey) {
		// Check if feature is enabled or not.
		if(!Shop.getPlugin().getConfig().getBoolean("displayFloatingCreateText") || !this.display.isEnabled()){
			ShopMessage.request(subkey, new PlaceholderContext().setProcess(this).setPlayer(player)).sendToAudience(player);
			return;
		}
		// Build the lines
		String formatted = ShopMessage.formatPlainTextSingle(subkey, placeholderContext);
		//todo:jmd implement
		//List<Component> lines = UtilMethods.splitStringIntoLines(formatted, ShopMessage.getTargetMaxLength());
		// Display the lines
		displayFloatingLines(List.of(Component.empty()));
	}
	
	public void displayFloatingTextList(String subkey) {
		// Check if feature is enabled or not.
		if(!Shop.getPlugin().getConfig().getBoolean("displayFloatingCreateText") || !this.display.isEnabled()){
			for(String message : ShopMessage.formatPlainText(subkey, this.placeholderContext)){
				if(message != null && !message.isEmpty()){
					Shop.getPlugin().getLangManager().request(message).sendToAudience(player);
				}
			}
			return;
		}
		List<Component> lines = new ArrayList<>();
		// Build the lines
		//for(String formatted : ShopMessage.formatPlainText(subkey, this.placeholderContext)){
		//	lines.addAll(UtilMethods.splitStringIntoLines(formatted, ShopMessage.getTargetMaxLength()));
		//}
		
		//todo:jmd implement
		// Display the lines
		displayFloatingLines(lines);
	}
	
	public void displayFloatingLines(List<Component> lines) {
		if(!this.display.isEnabled()){
			Shop.getPlugin().getLogger().warning("Unable to display floating text for player " + player.getName() + ", Display is disabled");
			return;
		}
		// Remove any existing text
		this.display.removeDisplayEntities(player, true);
		
		Location loc = this.container.getLocation().clone().add(0.5, 0.625 + (0.248 * lines.size()), 0.5);
		int i = 0;
		for(var line : lines){
			this.display.createTagEntity(player, line, loc.clone().add(0, (i * -0.248), 0));
			i++;
		}
	}
	
	public enum ChatCreationStep{
		// Sign creation steps
		SIGN_CREATION,
		SIGN_ITEM,
		SIGN_BARTER_ITEM,
	}
	
}
