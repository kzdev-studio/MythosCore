package pl.mythoscore.server;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.mythoscore.entity.MythosPlayer;

public class MythosChatListener {

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayerEntity fabricPlayer = sender;
            Player bukkitPlayer = MythosPlayer.wrap(fabricPlayer);
            String text = message.getContent().getString();

            java.util.Set<Player> recipients = new java.util.HashSet<>();
            if (Bukkit.getOnlinePlayers() != null) {
                recipients.addAll(Bukkit.getOnlinePlayers());
            } else {
                recipients.add(bukkitPlayer);
            }

            AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(true, bukkitPlayer, text, recipients);
            Bukkit.getPluginManager().callEvent(chatEvent);

            if (chatEvent.isCancelled()) {
                return false;
            }

            // ZAWSZE używamy formatu z pluginu (jeśli istnieje) lub domyślnego
            String format = chatEvent.getFormat();
            if (format == null) format = "<%1$s> %2$s";

            String finalMessage = String.format(format, bukkitPlayer.getName(), chatEvent.getMessage());
            fabricPlayer.server.getPlayerManager().broadcast(net.minecraft.text.Text.literal(finalMessage), false);

            return false; // Blokujemy oryginalną wiadomość, bo wysłaliśmy sformatowaną
        });
    }
}