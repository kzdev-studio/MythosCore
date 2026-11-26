package pl.mythoscore.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Vector3f;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.mixin.DisplayEntityMixin;
import pl.mythoscore.mixin.TextDisplayMixin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MythosNametagManager - tworzy i aktualizuje napis nad głową gracza (TextDisplayEntity),
 * który jest widoczny zarówno dla innych graczy, jak i dla samego gracza w trybie F5!
 *
 * Dlaczego TextDisplayEntity?
 * W czystym Minecrafcie klient gry ma blokadę w LivingEntityRenderer.hasLabel(),
 * która sprawia, że zwykły nametag ze Scoreboardu NIGDY nie rysuje się nad własną głową w F5.
 * Byt DisplayEntity.TextDisplayEntity (wprowadzony w 1.19.4 / 1.20.1) posadzony jako pasażer
 * (startRiding) na graczu omija to ograniczenie z zerowym opóźnieniem (0 lagów przy chodzeniu)
 * i wyświetla rangę + nick także w F5!
 */
public class MythosNametagManager {

    public static final String NAMETAG_TAG = "mythos_nametag";
    private static final Map<UUID, DisplayEntity.TextDisplayEntity> NAMETAGS = new HashMap<>();
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static int tickCounter = 0;

    public static void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 20 != 0) return; // Co 1 sekunde

        ServerScoreboard board = server.getScoreboard();

        // Przywracamy standardowa widocznosc nametagow dla wszystkich teamow Scoreboardu
        for (Team team : board.getTeams()) {
            if (team.getNameTagVisibilityRule() != AbstractTeam.VisibilityRule.ALWAYS) {
                team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
                board.updateScoreboardTeam(team);
            }
        }

        // Usuwamy wszelkie osierocone byty TextDisplay, ktore mogly zostac na spawnie lub w swiecie
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity.getCommandTags().contains(NAMETAG_TAG)) {
                    entity.discard();
                }
            }
        }
    }

    private static void updatePlayerNametag(MinecraftServer server, ServerScoreboard board, ServerPlayerEntity player) {
        // Zastapione przez natywny system Scoreboard (AethelRanks) bez duplikatow i ghost entity.
    }

    public static void removeNametag(UUID uuid) {
        DisplayEntity.TextDisplayEntity display = NAMETAGS.remove(uuid);
        LAST_TEXT.remove(uuid);
        if (display != null && !display.isRemoved()) {
            display.discard();
        }
    }
}
