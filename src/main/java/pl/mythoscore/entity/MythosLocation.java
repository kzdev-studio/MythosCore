package pl.mythoscore.entity;

import net.minecraft.server.world.ServerWorld;
import org.bukkit.Location;
import org.bukkit.World;

public class MythosLocation {

    public static Location wrap(ServerWorld world, double x, double y, double z) {
        World bukkitWorld = MythosWorld.wrap(world);
        // Tworzymy PRAWDZIWY obiekt Location, bez Proxy
        return new Location(bukkitWorld, x, y, z);
    }
}