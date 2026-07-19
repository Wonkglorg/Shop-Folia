package com.snowgears.shop.util;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemNameUtil {

    public ItemNameUtil() { }

    public String translate(String key){
        return new TranslatableComponent(key).toPlainText();
    }

    public TextComponent getName(ItemStack item){
        if(item == null)
            return new TextComponent("");


        // Check if there is a name embedded in the item, aka named by an anvil or command
        if(item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && !item.getItemMeta().getDisplayName().isEmpty()){
            return (TextComponent) ShopMessage.componentFromLegacy(item.getItemMeta().getDisplayName());
        }

        // Add custom formatting for player heads
        if(item.getItemMeta() != null && item.getItemMeta() instanceof SkullMeta){
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta.getOwningPlayer() != null) {
                return new TextComponent(skullMeta.getOwnerProfile().getName() + "'s Head");
            }
        }

        // Add support for displaying smithing template types
        if(item.getItemMeta() != null) {
            String itemType = item.getType().name();
            if(itemType.endsWith("_SMITHING_TEMPLATE")) {
                String templateType = itemType.replace("_SMITHING_TEMPLATE", "");
                // Extract the template pattern name (e.g., "EYE" from "EYE_ARMOR_TRIM_SMITHING_TEMPLATE")
                if(templateType.endsWith("_ARMOR_TRIM")) {
                    ChatColor trimNameColor = ChatColor.YELLOW;
                    // Aqua: "Vex", "Spire", "Eye" and "Ward"
                    if (templateType.contains("VEX") || templateType.contains("SPIRE") || templateType.contains("EYE") || templateType.contains("WARD")) {
                        trimNameColor = ChatColor.AQUA;
                    } else if (templateType.contains("SILENCE")) {  trimNameColor = ChatColor.LIGHT_PURPLE; }
                    String formattedName = UtilMethods.capitalize(templateType.toLowerCase().replace("_", " "));
                    return new TextComponent(trimNameColor.toString() + formattedName);
                } else if(templateType.equals("NETHERITE_UPGRADE")) {
                    return new TextComponent(ChatColor.YELLOW.toString() + "Netherite Upgrade Template");
                } else {
                    // For any other potential smithing templates
                    String formattedName = UtilMethods.capitalize(templateType.toLowerCase().replace("_", " "));
                    return new TextComponent(ChatColor.YELLOW.toString() + formattedName);
                }
            }
        }

        // Add custom potion formatting
        if(item.getItemMeta() != null && item.getItemMeta() instanceof PotionMeta){
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            if (potionMeta.getBasePotionType() != null) {
                String formattedName = UtilMethods.capitalize(item.getType().name().replace("_", " ").toLowerCase());
                formattedName += " of ";
                formattedName += UtilMethods.capitalize(potionMeta.getBasePotionType().toString().replace("_", " ").toLowerCase());
                return new TextComponent(formattedName);
            }
        }

        try {
            // Ominous Bottle's are Yellow *shrug*
            if (item.getItemMeta() != null && item.getItemMeta() instanceof org.bukkit.inventory.meta.OminousBottleMeta) {
                TextComponent name = getNameTranslatable(item.getType());
                name.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                return name;
            }
        } catch (Exception e) {} catch (Error e) {} // Backwards compatibility

        // Fallback to the material name
        return getNameTranslatable(item.getType());
    }

    public static TextComponent getNameTranslatable(Material material){
        if (!MCVersion.isTranslationSupported()) {
            return new TextComponent(UtilMethods.capitalize(material.name().toLowerCase().replace("_", " ")));
        }
        return new TextComponent(new TranslatableComponent(material.getTranslationKey()));
    }

    public static TextComponent getEnchantmentTranslatable(Enchantment enchantment){
        // Enchantment `getTranslationKey()` was added in 1.20.4, much later than Material/everywhere else...
        if (!MCVersion.atLeast("1.20.4")) {
            return new TextComponent(getEnchantmentName(enchantment));
        }
        return new TextComponent(new TranslatableComponent(enchantment.getTranslationKey()));
    }

    // Legacy method for getting the enchantment name
    public static String getEnchantmentName(Enchantment enchantment){
        //        System.out.println(enchantment.getName());
        //        System.out.println(enchantment.getKey().getKey());
        //        System.out.println(enchantment.getKey().getNamespace());
        switch (enchantment.getName()) {
            case "ARROW_DAMAGE":
                return "Power";
            case "ARROW_FIRE":
                return "Flame";
            case "ARROW_INFINITE":
                return "Infinity";
            case "ARROW_KNOCKBACK":
                return "Punch";
            case "BINDING_CURSE":
                return "Curse of Binding";
            case "CHANNELING":
                return "Channeling";
            case "DAMAGE_ALL":
                return "Sharpness";
            case "DAMAGE_ARTHROPODS":
                return "Bane of Arthropods";
            case "DAMAGE_UNDEAD":
                return "Smite";
            case "DEPTH_STRIDER":
                return "Depth Strider";
            case "DIG_SPEED":
                return "Efficiency";
            case "DURABILITY":
                return "Unbreaking";
            case "FIRE_ASPECT":
                return "Fire Aspect";
            case "FROST_WALKER":
                return "Frost Walker";
            case "IMPALING":
                return "Impaling";
            case "KNOCKBACK":
                return "Knockback";
            case "LOOT_BONUS_BLOCKS":
                return "Fortune";
            case "LOOT_BONUS_MOBS":
                return "Looting";
            case "LOYALTY":
                return "Loyalty";
            case "LUCK":
                return "Luck of the Sea";
            case "LURE":
                return "Lure";
            case "MENDING":
                return "Mending";
            case "MULTISHOT":
                return "Multishot";
            case "OXYGEN":
                return "Respiration";
            case "PIERCING":
                return "Piercing";
            case "PROTECTION_ENVIRONMENTAL":
                return "Protection";
            case "PROTECTION_EXPLOSIONS":
                return "Blast Protection";
            case "PROTECTION_FALL":
                return "Feather Falling";
            case "PROTECTION_FIRE":
                return "Fire Protection";
            case "PROTECTION_PROJECTILE":
                return "Projectile Protection";
            case "QUICK_CHARGE":
                return "Quick Charge";
            case "RIPTIDE":
                return "Riptide";
            case "SILK_TOUCH":
                return "Silk Touch";
            case "SOUL_SPEED":
                return "Soul Speed";
            case "SWEEPING_EDGE":
                return "Sweeping Edge";
            case "SWIFT_SNEAK":
                return "Swift Sneak";
            case "THORNS":
                return "Thorns";
            case "VANISHING_CURSE":
                return "Cure of Vanishing";
            case "WATER_WORKER":
                return "Aqua Affinity";
            default:
                return UtilMethods.capitalize(enchantment.getName().toLowerCase().replace("_", " "));
        }
    }
}
