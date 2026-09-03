package com.wonkglorg.minecraft.shop.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wonkglorg.minecraft.command.AbstractCommand;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_INITIALIZE_COMMAND;
import static com.wonkglorg.minecraft.shop.Constants.SHOP_PERMISSION_USER;
import com.wonkglorg.minecraft.shop.ShopPlugin;
import static com.wonkglorg.minecraft.shop.ShopPlugin.langManager;
import static com.wonkglorg.minecraft.shop.ShopPlugin.shopManager;
import com.wonkglorg.minecraft.shop.shop.creation.ShopCreationProcess;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import static io.papermc.paper.command.brigadier.Commands.literal;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import static org.bukkit.Material.AIR;
import static org.bukkit.Material.CAVE_AIR;
import static org.bukkit.Material.ENCHANTED_BOOK;
import static org.bukkit.Material.LINGERING_POTION;
import static org.bukkit.Material.POTION;
import static org.bukkit.Material.SPLASH_POTION;
import static org.bukkit.Material.VOID_AIR;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ShopInitializeCommand extends AbstractCommand{
	
	private static final Set<Material> EXCLUDED_MATERIALS = Set.of(AIR, CAVE_AIR, VOID_AIR, POTION, SPLASH_POTION, LINGERING_POTION, ENCHANTED_BOOK);
	
	/**
	 * Normal items that can be selected directly.
	 *
	 * DIAMOND -> ItemStack(DIAMOND)
	 * STICK   -> ItemStack(STICK)
	 */
	private static final Map<String, ItemStack> SINGLE_SELECTION = new HashMap<>();
	
	/**
	 * Items requiring an additional argument.
	 *
	 * POTION -> speed -> ItemStack(POTION with speed)
	 * ENCHANTED_BOOK -> sharpness -> ItemStack(ENCHANTED_BOOK with sharpness)
	 */
	private static final Map<String, Map<String, ItemStack>> DOUBLE_SELECTION = new HashMap<>();
	
	private static final Map<String, PotionType> POTION_TYPES = new HashMap<>();
	
	private static final Map<String, Enchantment> ENCHANTMENTS = new HashMap<>();
	
	public ShopInitializeCommand() {
		initializeRegistries();
		initializeNormalItems();
		initializeSpecialItems();
	}
	
	private static void initializeRegistries() {
		//@formatter:off
		RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION)
					  .forEach(potionType -> POTION_TYPES.put(potionType.getKey().getKey().toLowerCase(), potionType));
		
		RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
					  .forEach(enchantment -> ENCHANTMENTS.put(enchantment.getKey().getKey().toLowerCase(), enchantment));
		//@formatter:on
	}
	
	private static void initializeNormalItems() {
		Arrays.stream(Material.values())
			  .filter(Material::isItem)
			  .filter(material -> !EXCLUDED_MATERIALS.contains(material))
			  .forEach(material -> SINGLE_SELECTION.put(material.name().toLowerCase(), new ItemStack(material)));
	}
	
	private static void initializeSpecialItems() {
		Map<String, ItemStack> potions = new HashMap<>();
		Map<String, ItemStack> splashPotions = new HashMap<>();
		Map<String, ItemStack> lingeringPotions = new HashMap<>();
		Map<String, ItemStack> enchantedBooks = new HashMap<>();
		
		POTION_TYPES.forEach((key, potionType) -> {
			potions.put(key, createPotion(POTION, potionType));
			
			splashPotions.put(key, createPotion(SPLASH_POTION, potionType));
			
			lingeringPotions.put(key, createPotion(LINGERING_POTION, potionType));
		});
		
		ENCHANTMENTS.forEach((key, enchantment) -> enchantedBooks.put(key, createEnchantedBook(enchantment)));
		
		DOUBLE_SELECTION.put(POTION.name().toLowerCase(), potions);
		DOUBLE_SELECTION.put(SPLASH_POTION.name().toLowerCase(), splashPotions);
		DOUBLE_SELECTION.put(LINGERING_POTION.name().toLowerCase(), lingeringPotions);
		DOUBLE_SELECTION.put(ENCHANTED_BOOK.name().toLowerCase(), enchantedBooks);
	}
	
	private static ItemStack createPotion(Material material, PotionType potionType) {
		ItemStack item = new ItemStack(material);
		
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		if(meta == null){
			throw new IllegalStateException("Unable to create PotionMeta for " + material);
		}
		
		meta.setBasePotionType(potionType);
		item.setItemMeta(meta);
		
		return item;
	}
	
	private static ItemStack createEnchantedBook(Enchantment enchantment) {
		ItemStack item = new ItemStack(ENCHANTED_BOOK);
		
		EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
		
		if(meta == null){
			throw new IllegalStateException("Unable to create EnchantmentStorageMeta");
		}
		
		meta.addStoredEnchant(enchantment, 1, true);
		item.setItemMeta(meta);
		
		return item;
	}
	
	@Override
	public LiteralArgumentBuilder<CommandSourceStack> argumentBuilder() {
		LiteralArgumentBuilder<CommandSourceStack> builder = literal(SHOP_INITIALIZE_COMMAND).requires(permissions(SHOP_PERMISSION_USER));
		builder.executes(handler());
		
		SINGLE_SELECTION.forEach((key, item) -> builder.then(literal(key).executes(handler(key, null, false))));
		ShopPlugin.getPlugin().getItemConfig().getCustomItems().forEach((key, item) -> builder.then(literal(key).executes(handler(key, null, true))));
		DOUBLE_SELECTION.forEach((itemType, selections) -> {
			
			LiteralArgumentBuilder<CommandSourceStack> itemBuilder = literal(itemType);
			
			selections.forEach((extra, item) -> itemBuilder.then(literal(extra).executes(handler(itemType, extra, false))));
			
			builder.then(itemBuilder);
		});
		
		return builder;
	}
	
	private Command<CommandSourceStack> handler() {
		return command -> {
			
			if(!(command.getSource().getSender() instanceof Player player)){
				return -1;
			}
			
			ShopCreationProcess process = shopManager().getShopCreationProcess(player);
			
			if(process == null){
				langManager().request("command.shop-initialize.no-initialisation").sendToAudience(player);
				return -1;
			}
			
			ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
			if(item.getType() == AIR){
				langManager().request("command.shop-initialize.not-valid-item").sendToAudience(player);
				return -1;
			}
			shopManager().shopInitialisation(process, player, item.clone());
			return 1;
		};
	}
	
	private Command<CommandSourceStack> handler(String key, String extra, boolean customItem) {
		return command -> {
			
			if(!(command.getSource().getSender() instanceof Player player)){
				return -1;
			}
			
			ShopCreationProcess process = shopManager().getShopCreationProcess(player);
			
			if(process == null){
				langManager().request("command.shop-initialize.no-initialisation").sendToAudience(player);
				return -1;
			}
			
			ItemStack item;
			
			if(customItem){
				// Custom item handling should use the custom item
				// configuration rather than the normal material maps.
				//todo insert the custom item key id into the database to allow for self updating shops when definitions change!
				item = ShopPlugin.getPlugin().getItemConfig().getCustomItems().get(key);
			} else if(extra == null){
				item = SINGLE_SELECTION.get(key);
			} else {
				Map<String, ItemStack> selections = DOUBLE_SELECTION.get(key);
				
				if(selections == null){
					langManager().request("command.shop-initialize.not-valid-item").sendToAudience(player);
					return -1;
				}
				
				item = selections.get(extra);
			}
			
			if(item == null){
				langManager().request("command.shop-initialize.not-valid-item").sendToAudience(player);
				return -1;
			}
			
			shopManager().shopInitialisation(process, player, item.clone());
			return 1;
		};
	}
	
	@Override
	public String description() {
		return "Initializes a currently waiting shop with the specified item.";
	}
}