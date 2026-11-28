package pl.mythoscore.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

import java.util.Locale;

public final class MythosMaterials {

    private MythosMaterials() {
    }

    public static Material fromFabricItem(Item item) {
        if (item == null || item == Items.AIR) {
            return Material.AIR;
        }
        Identifier id = Registries.ITEM.getId(item);
        if ("minecraft".equals(id.getNamespace())) {
            Material matched = Material.matchMaterial(id.getPath());
            if (matched != null) {
                return matched;
            }
        }
        return (item instanceof BlockItem) ? Material.STONE : Material.PAPER;
    }

    public static Material fromFabricBlock(Block block) {
        if (block == null || block == Blocks.AIR) {
            return Material.AIR;
        }
        if (block == Blocks.CAVE_AIR) {
            return Material.CAVE_AIR;
        }
        if (block == Blocks.VOID_AIR) {
            return Material.VOID_AIR;
        }
        Identifier id = Registries.BLOCK.getId(block);
        if ("minecraft".equals(id.getNamespace())) {
            Material matched = Material.matchMaterial(id.getPath());
            if (matched != null) {
                return matched;
            }
        }
        return Material.STONE;
    }

    public static Item toFabricItem(Material material) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return Items.AIR;
        }
        Identifier id = Identifier.tryParse("minecraft:" + material.name().toLowerCase(Locale.ROOT));
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (item != Items.AIR) {
                return item;
            }
        }
        return Items.STONE;
    }

    public static Block toFabricBlock(Material material) {
        if (material == null || material == Material.AIR) {
            return Blocks.AIR;
        }
        if (material == Material.CAVE_AIR) {
            return Blocks.CAVE_AIR;
        }
        if (material == Material.VOID_AIR) {
            return Blocks.VOID_AIR;
        }
        Identifier id = Identifier.tryParse("minecraft:" + material.name().toLowerCase(Locale.ROOT));
        if (id != null) {
            Block block = Registries.BLOCK.get(id);
            if (block != Blocks.AIR) {
                return block;
            }
        }
        return Blocks.STONE;
    }

    public static NamespacedKey toNamespacedKey(Identifier id) {
        if (id == null) {
            return NamespacedKey.minecraft("unknown");
        }
        try {
            return new NamespacedKey(id.getNamespace(), id.getPath());
        } catch (IllegalArgumentException e) {
            return NamespacedKey.minecraft("unknown");
        }
    }

    public static EntityType toBukkitEntityType(net.minecraft.entity.EntityType<?> mcType) {
        if (mcType == null) {
            return EntityType.UNKNOWN;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(mcType);
        if ("minecraft".equals(id.getNamespace())) {
            EntityType byName = EntityType.fromName(id.getPath());
            if (byName != null) {
                return byName;
            }
            try {
                return EntityType.valueOf(id.getPath().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return EntityType.UNKNOWN;
    }
}
