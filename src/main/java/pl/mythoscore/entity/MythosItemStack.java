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

    // Tłumaczy Bukkit ItemStack na Minecraft ItemStack
    public static ItemStack toFabric(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null || bukkitItem.getType() == Material.AIR) {
            return ItemStack.EMPTY;
        }
        net.minecraft.item.Item mcItem = MythosMaterials.toFabricItem(bukkitItem.getType());
        ItemStack stack = new ItemStack(mcItem, Math.max(1, bukkitItem.getAmount()));
        try {
            if (bukkitItem.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = bukkitItem.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    stack.setCustomName(net.minecraft.text.Text.literal(meta.getDisplayName()));
                }
            }
        } catch (Throwable ignored) {
        }
        return stack;
    }

    // Tłumaczy Minecraft ItemStack na Bukkit ItemStack (bezpieczny fallback dla modów)
    public static org.bukkit.inventory.ItemStack fromFabric(ItemStack fabricItem) {
        if (fabricItem == null || fabricItem.isEmpty()) {
            return new org.bukkit.inventory.ItemStack(Material.AIR);
        }
        Material mat = MythosMaterials.fromFabricItem(fabricItem.getItem());
        int amount = Math.max(1, fabricItem.getCount());
        String customName = fabricItem.hasCustomName() ? fabricItem.getName().getString() : null;
        return create(mat, amount, customName);
    }
}