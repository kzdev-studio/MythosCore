package pl.mythoscore.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.mythoscore.entity.MythosLocation;
import pl.mythoscore.entity.MythosPlayer;
import pl.mythoscore.entity.MythosWorld;

@Mixin(ServerPlayNetworkHandler.class)
public class PlayerMoveMixin {

    @Shadow public ServerPlayerEntity player;

    @Unique
    private double mythos$lastX = Double.NaN;
    @Unique
    private double mythos$lastY = Double.NaN;
    @Unique
    private double mythos$lastZ = Double.NaN;
    @Unique
    private float mythos$lastYaw = Float.NaN;
    @Unique
    private float mythos$lastPitch = Float.NaN;

    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void mythos$onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (this.player == null) return;
        double x = this.player.getX();
        double y = this.player.getY();
        double z = this.player.getZ();
        float yaw = this.player.getYaw();
        float pitch = this.player.getPitch();

        if (Double.isNaN(mythos$lastX)) {
            mythos$lastX = x;
            mythos$lastY = y;
            mythos$lastZ = z;
            mythos$lastYaw = yaw;
            mythos$lastPitch = pitch;
            return;
        }

        if (x == mythos$lastX && y == mythos$lastY && z == mythos$lastZ
                && yaw == mythos$lastYaw && pitch == mythos$lastPitch) {
            return;
        }

        ServerWorld sw = this.player.getServerWorld();
        Location from = MythosLocation.wrap(sw, mythos$lastX, mythos$lastY, mythos$lastZ, mythos$lastYaw, mythos$lastPitch);
        Location to = MythosLocation.wrap(sw, x, y, z, yaw, pitch);
        Player bukkitPlayer = MythosPlayer.wrap(this.player);

        PlayerMoveEvent event = new PlayerMoveEvent(bukkitPlayer, from, to);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            this.player.teleport(sw, from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
            return;
        }

        mythos$lastX = x;
        mythos$lastY = y;
        mythos$lastZ = z;
        mythos$lastYaw = yaw;
        mythos$lastPitch = pitch;
    }
}
