package pl.mythoscore.mixin;

import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.command.CommandSender;
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

        // Jeśli komenda zaczyna się od "/" (np. "//wand" lub "/worldedit"),
        // to ZOSTAWIAMY JĄ W SPOKOJU. Nie chcemy jej przechwytywać.
        if (command.startsWith("/")) {
            return;
        }

        // Przechwytujemy tylko komendy bez ukośnika (np. "pomoc", "sklep")
        String cmdName = command.split(" ")[0].toLowerCase();

        if (MythosServer.getCommandMap().getKnownCommands().containsKey(cmdName)) {
            MythosCoreMod.LOGGER.info("Przechwycono komende z pakietu: {}", command);

            CommandSender sender = MythosPlayer.wrap(this.player);
            MythosServer.getCommandMap().dispatch(sender, command);

            ci.cancel();
        }
    }
}