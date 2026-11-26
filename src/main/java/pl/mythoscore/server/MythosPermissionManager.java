package pl.mythoscore.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MythosPermissionManager {

    private final MythosConfig config;
    private final Map<String, String> playerRanks = new HashMap<>();

    public MythosPermissionManager() {
        // Plik zapisze się w run/plugins/mythoscore/ranks.yml
        this.config = new MythosConfig(new File("plugins/mythoscore/ranks.yml"));
        loadRanks();
    }

    @SuppressWarnings("unchecked")
    private void loadRanks() {
        Object ranksObj = config.get("ranks");
        if (ranksObj instanceof Map) {
            playerRanks.putAll((Map<String, String>) ranksObj);
        }
    }

    public void setRank(String playerName, String rank) {
        playerRanks.put(playerName.toLowerCase(), rank);
        config.set("ranks", playerRanks);
    }

    public String getRank(String playerName) {
        return playerRanks.getOrDefault(playerName.toLowerCase(), "Gracz");
    }

    // --- NOWA METODA: Sprawdzanie uprawnień ---
    public boolean hasPermission(String playerName, String permission) {
        String rank = getRank(playerName);

        // VIP ma wszystkie uprawnienia (dla testów)
        if (rank.equalsIgnoreCase("VIP")) {
            return true;
        }

        // Zwykły gracz ma tylko podstawowe uprawnienia
        return permission.equalsIgnoreCase("aethelgard.basic");
    }
}