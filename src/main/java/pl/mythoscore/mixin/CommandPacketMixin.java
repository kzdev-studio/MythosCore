package pl.mythoscore.mixin;

import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.entity.MythosPlayer;
import pl.mythoscore.server.MythosServer;

@Mixin(ServerPlayNetworkHandler.class)
public class CommandPacketMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onCommandExecution", at = @At("HEAD"), cancellable = true)
    private void onCommandExecution(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        String command = packet.command();
        Player bukkitPlayer = MythosPlayer.wrap(this.player);

        PlayerCommandPreprocessEvent preprocessEvent = new PlayerCommandPreprocessEvent(bukkitPlayer, "/" + command);
        Bukkit.getPluginManager().callEvent(preprocessEvent);
        if (preprocessEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        String processed = preprocessEvent.getMessage();
        if (processed != null && processed.startsWith("/")) {
            command = processed.substring(1);
        }

        // Jeśli komenda nadal zaczyna się od "/" (np. "//wand"), zostawiamy dla modów
        if (command.startsWith("/")) {
            return;
        }

        String cmdName = command.split(" ")[0].toLowerCase();

        if (MythosServer.getCommandMap().getKnownCommands().containsKey(cmdName)) {
            MythosCoreMod.LOGGER.info("Przechwycono komende z pakietu: {}", command);

            CommandSender sender = bukkitPlayer;
            MythosServer.getCommandMap().dispatch(sender, command);

            ci.cancel();
        }
    }
}