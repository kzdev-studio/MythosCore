package pl.mythoscore.server;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.entity.MythosBlock;
import pl.mythoscore.entity.MythosItemStack;
import pl.mythoscore.entity.MythosPlayer;

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

            ServerWorld sw = serverPlayer.getServerWorld();
            Block bukkitBlock = MythosBlock.wrap(sw, pos);
            BlockBreakEvent event = new BlockBreakEvent(bukkitBlock, bukkitPlayer);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled() ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 2. Stawianie bloku i interakcja z blokiem
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            Player bukkitPlayer = MythosPlayer.wrap(serverPlayer);
            String worldName = serverPlayer.getWorld().getRegistryKey().getValue().getPath();
            BlockPos pos = hitResult.getBlockPos();
            ServerWorld sw = serverPlayer.getServerWorld();
            Block bukkitBlock = MythosBlock.wrap(sw, pos);
            EquipmentSlot eqSlot = (hand == Hand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
            ItemStack itemInHand = MythosItemStack.fromFabric(player.getStackInHand(hand));

            // Sprawdzamy, czy gracz próbuje POSTAWIĆ blok
            if (player.getStackInHand(hand).getItem() instanceof net.minecraft.item.BlockItem) {
                if (!MythosCoreMod.claimManager.canBuild(worldName, pos.getX(), pos.getZ(), bukkitPlayer.getName())) {
                    bukkitPlayer.sendMessage("§cTen teren należy do kogoś innego!");
                    return ActionResult.FAIL;
                }

                BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                        bukkitBlock, bukkitBlock.getState(), bukkitBlock,
                        itemInHand, bukkitPlayer, true, eqSlot
                );
                Bukkit.getPluginManager().callEvent(placeEvent);
                if (placeEvent.isCancelled()) return ActionResult.FAIL;
            }

            PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                    bukkitPlayer, Action.RIGHT_CLICK_BLOCK, itemInHand, bukkitBlock,
                    toBlockFace(hitResult.getSide()), eqSlot
            );
            Bukkit.getPluginManager().callEvent(interactEvent);
            return interactEvent.isCancelled() ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 3. Interakcja w powietrzu (prawy klik itemem)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.item.ItemStack mcStack = player.getStackInHand(hand);
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(mcStack);
            }
            Player bukkitPlayer = MythosPlayer.wrap(serverPlayer);
            EquipmentSlot eqSlot = (hand == Hand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
            ItemStack itemInHand = MythosItemStack.fromFabric(mcStack);

            PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                    bukkitPlayer, Action.RIGHT_CLICK_AIR, itemInHand, null, BlockFace.SELF, eqSlot
            );
            Bukkit.getPluginManager().callEvent(interactEvent);
            return interactEvent.isCancelled() ? TypedActionResult.fail(mcStack) : TypedActionResult.pass(mcStack);
        });
    }

    private static BlockFace toBlockFace(Direction dir) {
        if (dir == null) return BlockFace.UP;
        return switch (dir) {
            case DOWN -> BlockFace.DOWN;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
            default -> BlockFace.UP;
        };
    }
}