package com.wonkglorg.minecraft.shop.shop.creation;

import com.wonkglorg.minecraft.config.lang.LangRequest;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.logger;
import static com.wonkglorg.minecraft.shop.shop.AbstractShop.formatPrice;
import static com.wonkglorg.minecraft.shop.shop.ShopState.OK;
import com.wonkglorg.minecraft.shop.shop.creation.SignCreationLayoutParser.CreationMatch;
import com.wonkglorg.minecraft.shop.util.ItemNameUtil;
import com.wonkglorg.minecraft.util.Components;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SignCreationProcess extends ShopCreationProcess{
	public SignCreationProcess(Player player, Sign sign, Block container, BlockFace signDirection) {
		super(player, sign, container, signDirection);
		isFakeSign = false;
	}
	
	/**
	 * Reads and validates the four sign lines.
	 */
	public boolean readSignLines(List<Component> components) {
		if(components.size() < 4){
			return false;
		}
		
		String[] lines = new String[4];
		
		for(int i = 0; i < 4; i++){
			lines[i] = Components.toPlainText(components.get(i)).trim();
		}
		
		CreationMatch match = SignCreationLayoutParser.match(lines);
		if(match == null){
			logger().debug("No match Found for lines!");
			return false;
		}
		amount = match.amount();
		price = match.price();
		adminShop = match.admin();
		type = match.shopType();
		
		if(!isAllowedToCreateShop()){
			logger().debug("Player is not allowed to create a shop of this type!");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Gets the initialize context sign lines
	 */
	private static List<Component> getSignLines(ShopCreationProcess context) {
		List<Component> lines = new ArrayList<>(4);
		for(var i = 1; i < 5; i++){
			//@formatter:off
			LangRequest request = langManager().request("sign.text." + context.getType() + ".initialise." + i);
			
			if(context.getItemStack() != null){
				request.replace("%item%",() -> ItemNameUtil.getName(context.getItemStack()));
			}else{
				request.replace("%item%","");
			}
			
			if(context.getSecondaryStack() != null){
				request.replace("%barter-item%",()->ItemNameUtil.getName(context.getSecondaryStack()));
			}else{
				request.replace("%barter-item%","");
			}
			
			request.replace("%amount%",context.getAmount())
				   .replace("%stock-state%",OK)
				   .replace("%price%",formatPrice(context.getPrice()))
				   .replace("%owner%",context.getPlayer().getName())
				   .replace("%stock%",0);
			lines.add(request.toSingleComponent());
			//@formatter:on
		}
		
		return lines;
	}
	
	public void updateSignText() {
		//schedule one tick later, otherwise sign change event can overwrite the text
		ShopPlugin.getPlugin().getFoliaLib().getScheduler().runAtLocationLater(sign.getLocation(), () -> {
			if(!(sign.getBlock().getState() instanceof Sign currentSign)){
				return;
			}
			
			currentSign.setWaxed(true);
			
			SignSide side = currentSign.getSide(Side.FRONT);
			List<Component> lines = getSignLines(this);
			
			for(int i = 0; i < 4; i++){
				side.line(i, lines.get(i));
			}
			
			currentSign.update(true);
		}, 1);
	}
	
	@Override
	public String toString() {
		return "SignCreationProcess{" +
			   "player=" +
			   player +
			   ", playerIsOperator=" +
			   playerIsOperator +
			   ", playerUUID=" +
			   playerUUID +
			   ", shopId=" +
			   shopId +
			   ", sign=" +
			   sign +
			   ", container=" +
			   container +
			   ", signDirection=" +
			   signDirection +
			   ", type=" +
			   type +
			   ", amount=" +
			   amount +
			   ", price=" +
			   price +
			   ", adminShop=" +
			   adminShop +
			   ", isFakeSign=" +
			   isFakeSign +
			   ", itemStack=" +
			   itemStack +
			   ", secondaryStack=" +
			   secondaryStack +
			   ", finishedInitialisation=" +
			   finishedInitialisation +
			   ", isCancelled=" +
			   isCancelled +
			   '}';
	}
}