package pl.mythoscore.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.bukkit.Material;

public class MythosItemStack {

    // Tworzymy normalny przedmiot Bukkita (bez Proxy!)
    public static org.bukkit.inventory.ItemStack create(Material material, int amount, String displayName) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material, amount);

        // Próbujemy ustawić nazwę (jeśli Meta działa)
        try {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null && displayName != null) {
                meta.setDisplayName(displayName);
                item.setItemMeta(meta);
            }
        } catch (Exception ignored) {
            // Jeśli Bukkit nie potrafi stworzyć Meta, po prostu zwracamy przedmiot bez nazwy
        }

        return item;
    }

    // Tłumaczy Bukkit Material na Minecraft Item
    public static ItemStack toFabric(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem.getType() == Material.DIAMOND) return new ItemStack(Items.DIAMOND);
        if (bukkitItem.getType() == Material.EMERALD) return new ItemStack(Items.EMERALD);
        if (bukkitItem.getType() == Material.GOLD_INGOT) return new ItemStack(Items.GOLD_INGOT);
        if (bukkitItem.getType() == Material.PAPER) return new ItemStack(Items.PAPER);
        return new ItemStack(Items.STONE);
    }
}