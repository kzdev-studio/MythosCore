package pl.mythoscore.server;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.entity.MythosPlayer;
import java.lang.reflect.Proxy;

public class MythosBlockListener {
    public static void register() {
        // 1. Niszczenie bloku
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            Player bukkitPlayer = MythosPlayer.wrap(serverPlayer);
            String worldName = serverPlayer.getWorld().getRegistryKey().getValue().getPath();

            if (!MythosCoreMod.claimManager.canBuild(worldName, pos.getX(), pos.getZ(), bukkitPlayer.getName())) {
                bukkitPlayer.sendMessage("§cTen teren należy do kogoś innego!");
                return ActionResult.FAIL;
            }

            BlockBreakEvent event = new BlockBreakEvent(createFakeBlock(pos), bukkitPlayer);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled() ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 2. Stawianie bloku i interakcja
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            Player bukkitPlayer = MythosPlayer.wrap(serverPlayer);
            String worldName = serverPlayer.getWorld().getRegistryKey().getValue().getPath();
            BlockPos pos = hitResult.getBlockPos();
            Block bukkitBlock = createFakeBlock(pos);

            // Sprawdzamy, czy gracz próbuje POSTAWIĆ blok
            if (player.getStackInHand(hand).getItem() instanceof net.minecraft.item.BlockItem) {
                if (!MythosCoreMod.claimManager.canBuild(worldName, pos.getX(), pos.getZ(), bukkitPlayer.getName())) {
                    bukkitPlayer.sendMessage("§cTen teren należy do kogoś innego!");
                    return ActionResult.FAIL;
                }

                // ZABEZPIECZENIE: Podajemy pusty przedmiot, bo nie mamy jeszcze ekwipunku
                ItemStack safeItem = new ItemStack(Material.STONE);

                BlockPlaceEvent placeEvent = new BlockPlaceEvent(bukkitBlock, bukkitBlock.getState(), bukkitBlock,
                        safeItem, bukkitPlayer, true, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(placeEvent);
                if (placeEvent.isCancelled()) return ActionResult.FAIL;
            }

            // ZABEZPIECZENIE: Podajemy pusty przedmiot dla interakcji
            ItemStack safeItem = new ItemStack(Material.AIR);

            PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                    bukkitPlayer, Action.RIGHT_CLICK_BLOCK, safeItem, bukkitBlock,
                    org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND
            );
            Bukkit.getPluginManager().callEvent(interactEvent);
            return interactEvent.isCancelled() ? ActionResult.FAIL : ActionResult.PASS;
        });
    }

    private static Block createFakeBlock(BlockPos pos) {
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class}, (proxy, method, args) -> {
            if (method.getName().equals("getType")) return Material.STONE;
            if (method.getName().equals("getX")) return pos.getX();
            if (method.getName().equals("getY")) return pos.getY();
            if (method.getName().equals("getZ")) return pos.getZ();
            if (method.getName().equals("getLocation")) return new org.bukkit.Location(null, pos.getX(), pos.getY(), pos.getZ());
            if (method.getName().equals("getState")) {
                return (BlockState) Proxy.newProxyInstance(BlockState.class.getClassLoader(), new Class<?>[]{BlockState.class}, (p, m, a) -> {
                    if (m.getName().equals("getType")) return Material.STONE;
                    if (m.getName().equals("getBlock")) return proxy;
                    return MythosServer.getSafeDefault(m.getReturnType());
                });
            }
            return MythosServer.getSafeDefault(method.getReturnType());
        });
    }
}