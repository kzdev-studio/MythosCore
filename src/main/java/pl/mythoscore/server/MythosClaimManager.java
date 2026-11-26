package pl.mythoscore.server;

import pl.mythoscore.MythosCoreMod;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MythosClaimManager {
    private final MythosConfig config;
    private final Map<String, String> claims = new HashMap<>();

    public MythosClaimManager() {
        this.config = new MythosConfig(new File("plugins/mythoscore/claims.yml"));
        loadClaims();
    }

    @SuppressWarnings("unchecked")
    private void loadClaims() {
        Object obj = config.get("claims");
        if (obj instanceof Map) claims.putAll((Map<String, String>) obj);
    }

    private String getKey(String world, int x, int z) {
        return world + "_" + (x >> 4) + "_" + (z >> 4);
    }

    public boolean claim(String world, int x, int z, String playerName) {
        String key = getKey(world, x, z);
        if (claims.containsKey(key)) return false;
        claims.put(key, playerName.toLowerCase());
        config.set("claims", claims);
        return true;
    }

    public boolean unclaim(String world, int x, int z, String playerName) {
        String key = getKey(world, x, z);
        String owner = claims.get(key);
        if (owner != null && owner.equalsIgnoreCase(playerName)) {
            claims.remove(key);
            config.set("claims", claims);
            return true;
        }
        return false;
    }

    public boolean canBuild(String world, int x, int z, String playerName) {
        String owner = claims.get(getKey(world, x, z));
        if (owner == null) return true;
        if (owner.equalsIgnoreCase(playerName)) return true;
        return MythosCoreMod.permissionManager.hasPermission(playerName, "aethelgard.admin");
    }
}