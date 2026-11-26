package pl.mythoscore.entity;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class MythosInventory {

    public static final Map<org.bukkit.inventory.Inventory, MythosInventory> INVENTORY_MAP = new HashMap<>();

    private final SimpleInventory fabricInventory;
    private final String title;

    public MythosInventory(String title, int size) {
        int adjustedSize = Math.max(9, (size / 9) * 9);
        this.fabricInventory = new SimpleInventory(adjustedSize);
        this.title = title;
    }

    // --- TO BYŁO BRAKUJĄCE ---
    public String getTitle() {
        return title;
    }
    // -------------------------

    public org.bukkit.inventory.Inventory getBukkitInventory() {
        return (org.bukkit.inventory.Inventory) Proxy.newProxyInstance(
                org.bukkit.inventory.Inventory.class.getClassLoader(),
                new Class<?>[]{org.bukkit.inventory.Inventory.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getSize")) return fabricInventory.size();
                    if (methodName.equals("getTitle")) return title;

                    if (methodName.equals("setItem")) {
                        int slot = (int) args[0];
                        org.bukkit.inventory.ItemStack bukkitItem = (org.bukkit.inventory.ItemStack) args[1];
                        if (bukkitItem != null) {
                            fabricInventory.setStack(slot, MythosItemStack.toFabric(bukkitItem));
                        } else {
                            fabricInventory.setStack(slot, net.minecraft.item.ItemStack.EMPTY);
                        }
                        return null;
                    }
                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }

    public static org.bukkit.inventory.Inventory createBukkitInventory(String title, int size) {
        MythosInventory inv = new MythosInventory(title, size);
        org.bukkit.inventory.Inventory bukkitInv = inv.getBukkitInventory();
        INVENTORY_MAP.put(bukkitInv, inv);
        return bukkitInv;
    }

    public void openFor(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
            ScreenHandlerType<GenericContainerScreenHandler> type = ScreenHandlerType.GENERIC_9X3;
            if (fabricInventory.size() == 9) type = ScreenHandlerType.GENERIC_9X1;
            else if (fabricInventory.size() == 18) type = ScreenHandlerType.GENERIC_9X2;
            else if (fabricInventory.size() == 36) type = ScreenHandlerType.GENERIC_9X4;
            else if (fabricInventory.size() == 45) type = ScreenHandlerType.GENERIC_9X5;
            else if (fabricInventory.size() == 54) type = ScreenHandlerType.GENERIC_9X6;
            return new GenericContainerScreenHandler(type, syncId, inv, fabricInventory, 3);
        }, Text.literal(title)));
    }
}