package pl.mythoscore.mixin;

import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.mythoscore.entity.MythosInventory;
import pl.mythoscore.entity.MythosItemStack;
import pl.mythoscore.entity.MythosPlayer;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;

@Mixin(ServerPlayNetworkHandler.class)
public class InventoryClickMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (this.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {

            Inventory bukkitInv = null;
            MythosInventory mythosInv = null;

            for (var entry : MythosInventory.INVENTORY_MAP.entrySet()) {
                bukkitInv = entry.getKey();
                mythosInv = entry.getValue();
                break;
            }

            if (bukkitInv == null || mythosInv == null) return;

            int slot = packet.getSlot();

            if (slot >= 0 && slot < bukkitInv.getSize()) {
                net.minecraft.item.ItemStack fabricStack = handler.getSlot(slot).getStack();
                ItemStack clickedItem = null;
                if (fabricStack != null && !fabricStack.isEmpty()) {
                    clickedItem = MythosItemStack.fromFabric(fabricStack);
                }

                final Inventory finalBukkitInv = bukkitInv;
                final String finalTitle = mythosInv.getTitle();
                final ItemStack finalClickedItem = clickedItem;
                final Player bukkitPlayer = MythosPlayer.wrap(this.player);
                final int clickedSlot = slot;

                Inventory dummyBottom = (Inventory) Proxy.newProxyInstance(
                        Inventory.class.getClassLoader(),
                        new Class<?>[]{Inventory.class},
                        (p, m, a) -> {
                            if (m.getName().equals("getSize")) return 36;
                            return MythosServer.getSafeDefault(m.getReturnType());
                        }
                );

                InventoryView view = new InventoryView() {
                    @Override public Inventory getTopInventory() { return finalBukkitInv; }
                    @Override public Inventory getBottomInventory() { return dummyBottom; }
                    @Override public HumanEntity getPlayer() { return bukkitPlayer; }
                    @Override public InventoryType getType() { return InventoryType.CHEST; }
                    @Override public String getTitle() { return finalTitle; }
                    @Override public String getOriginalTitle() { return finalTitle; }
                    @Override public void setTitle(String title) {}

                    @Override
                    public ItemStack getItem(int slot) {
                        if (slot == clickedSlot) return finalClickedItem;
                        return null;
                    }
                };

                InventoryClickEvent event = new InventoryClickEvent(
                        view,
                        InventoryType.SlotType.CONTAINER,
                        slot,
                        packet.getButton() == 0 ? ClickType.LEFT : ClickType.RIGHT,
                        InventoryAction.PICKUP_ALL
                );

                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    ci.cancel(); // Blokujemy pakiet
                    this.player.currentScreenHandler.syncState(); // Synchronizujemy ekwipunek, żeby diament nie zniknął
                }
            }
        }
    }

    @Inject(method = "onCloseHandledScreen", at = @At("HEAD"))
    private void mythos$onCloseHandledScreen(CloseHandledScreenC2SPacket packet, CallbackInfo ci) {
        if (this.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            Inventory bukkitInv = null;
            MythosInventory mythosInv = null;
            for (var entry : MythosInventory.INVENTORY_MAP.entrySet()) {
                bukkitInv = entry.getKey();
                mythosInv = entry.getValue();
                break;
            }
            if (bukkitInv == null || mythosInv == null) return;

            final Inventory finalBukkitInv = bukkitInv;
            final String finalTitle = mythosInv.getTitle();
            final Player bukkitPlayer = MythosPlayer.wrap(this.player);

            Inventory dummyBottom = (Inventory) Proxy.newProxyInstance(
                    Inventory.class.getClassLoader(),
                    new Class<?>[]{Inventory.class},
                    (p, m, a) -> {
                        if (m.getName().equals("getSize")) return 36;
                        return MythosServer.getSafeDefault(m.getReturnType());
                    }
            );

            InventoryView view = new InventoryView() {
                @Override public Inventory getTopInventory() { return finalBukkitInv; }
                @Override public Inventory getBottomInventory() { return dummyBottom; }
                @Override public HumanEntity getPlayer() { return bukkitPlayer; }
                @Override public InventoryType getType() { return InventoryType.CHEST; }
                @Override public String getTitle() { return finalTitle; }
                @Override public String getOriginalTitle() { return finalTitle; }
                @Override public void setTitle(String title) {}
            };

            Bukkit.getPluginManager().callEvent(new InventoryCloseEvent(view));
        }
    }
}