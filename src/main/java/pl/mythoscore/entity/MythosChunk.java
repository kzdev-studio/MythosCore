package pl.mythoscore.entity;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;

public class MythosChunk {

    public static Chunk wrap(ServerWorld world, int chunkX, int chunkZ) {
        return (Chunk) Proxy.newProxyInstance(
                Chunk.class.getClassLoader(),
                new Class<?>[]{Chunk.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getX")) return chunkX;
                    if (methodName.equals("getZ")) return chunkZ;
                    if (methodName.equals("getWorld")) return MythosWorld.wrap(world);
                    if (methodName.equals("isLoaded")) return world.isChunkLoaded(chunkX, chunkZ);
                    if (methodName.equals("load")) {
                        world.getChunk(chunkX, chunkZ);
                        return true;
                    }
                    if (methodName.equals("getBlock") && args != null && args.length >= 3
                            && args[0] instanceof Number x && args[1] instanceof Number y && args[2] instanceof Number z) {
                        int bx = (chunkX << 4) + (x.intValue() & 15);
                        int bz = (chunkZ << 4) + (z.intValue() & 15);
                        return MythosBlock.wrap(world, new BlockPos(bx, y.intValue(), bz));
                    }
                    if (methodName.equals("getEntities")) return new Entity[0];
                    if (methodName.equals("equals")) {
                        Object other = (args != null && args.length > 0) ? args[0] : null;
                        return other instanceof Chunk c
                                && c.getX() == chunkX
                                && c.getZ() == chunkZ
                                && MythosWorld.toWorldUuid(world).equals(c.getWorld().getUID());
                    }
                    if (methodName.equals("hashCode")) {
                        return (chunkX * 31 + chunkZ) * 31 + MythosWorld.toWorldUuid(world).hashCode();
                    }
                    if (methodName.equals("toString")) {
                        return "MythosChunk{" + chunkX + "," + chunkZ + "}";
                    }

                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }
}
