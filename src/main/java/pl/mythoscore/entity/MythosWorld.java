package pl.mythoscore.entity;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MythosWorld {

    private static final Map<UUID, ServerWorld> WORLD_MAP = new ConcurrentHashMap<>();

    public static ServerWorld getFabricWorld(World bukkitWorld) {
        if (bukkitWorld == null) return null;
        return WORLD_MAP.get(bukkitWorld.getUID());
    }

    public static String toBukkitName(ServerWorld fabricWorld) {
        String path = fabricWorld.getRegistryKey().getValue().getPath();
        if ("overworld".equals(path)) return "world";
        if ("the_nether".equals(path)) return "world_nether";
        if ("the_end".equals(path)) return "world_the_end";
        return path;
    }

    public static UUID toWorldUuid(ServerWorld fabricWorld) {
        return UUID.nameUUIDFromBytes(
                fabricWorld.getRegistryKey().getValue().toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    public static World wrap(ServerWorld fabricWorld) {
        String bukkitName = toBukkitName(fabricWorld);
        UUID worldUid = toWorldUuid(fabricWorld);
        WORLD_MAP.put(worldUid, fabricWorld);

        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getName")) return bukkitName;
                    if (methodName.equals("getUID")) return worldUid;
                    if (methodName.equals("getSpawnLocation")) {
                        return MythosLocation.wrap(
                                fabricWorld,
                                fabricWorld.getSpawnPos().getX(),
                                fabricWorld.getSpawnPos().getY(),
                                fabricWorld.getSpawnPos().getZ()
                        );
                    }
                    if (methodName.equals("getPlayers")) {
                        List<Player> players = new ArrayList<>();
                        for (ServerPlayerEntity sp : fabricWorld.getPlayers()) {
                            players.add(MythosPlayer.wrap(sp));
                        }
                        return players;
                    }
                    if (methodName.equals("getEnvironment")) {
                        String path = fabricWorld.getRegistryKey().getValue().getPath();
                        if ("the_nether".equals(path)) return World.Environment.NETHER;
                        if ("the_end".equals(path)) return World.Environment.THE_END;
                        return World.Environment.NORMAL;
                    }
                    if (methodName.equals("getSeed")) return fabricWorld.getSeed();
                    if (methodName.equals("getTime")) return fabricWorld.getTimeOfDay() % 24000L;
                    if (methodName.equals("getFullTime")) return fabricWorld.getTimeOfDay();
                    if (methodName.equals("setTime")) {
                        fabricWorld.setTimeOfDay((Long) args[0]);
                        return null;
                    }
                    if (methodName.equals("getMinHeight")) return fabricWorld.getBottomY();
                    if (methodName.equals("getMaxHeight")) return fabricWorld.getTopY();
                    if (methodName.equals("getBlockAt") && args != null) {
                        if (args.length == 3 && args[0] instanceof Number x && args[1] instanceof Number y && args[2] instanceof Number z) {
                            return MythosBlock.wrap(fabricWorld, new net.minecraft.util.math.BlockPos(x.intValue(), y.intValue(), z.intValue()));
                        }
                        if (args.length == 1 && args[0] instanceof org.bukkit.Location loc) {
                            return MythosBlock.wrap(fabricWorld, new net.minecraft.util.math.BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                        }
                    }
                    if (methodName.equals("getChunkAt") && args != null) {
                        if (args.length == 2 && args[0] instanceof Number cx && args[1] instanceof Number cz) {
                            return MythosChunk.wrap(fabricWorld, cx.intValue(), cz.intValue());
                        }
                        if (args.length == 1 && args[0] instanceof org.bukkit.Location loc) {
                            return MythosChunk.wrap(fabricWorld, loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
                        }
                        if (args.length == 1 && args[0] instanceof org.bukkit.block.Block blk) {
                            return MythosChunk.wrap(fabricWorld, blk.getX() >> 4, blk.getZ() >> 4);
                        }
                    }
                    if (methodName.equals("isChunkLoaded") && args != null && args.length == 2 && args[0] instanceof Number cx && args[1] instanceof Number cz) {
                        return fabricWorld.isChunkLoaded(cx.intValue(), cz.intValue());
                    }
                    if (methodName.equals("loadChunk") && args != null && args.length >= 2 && args[0] instanceof Number cx && args[1] instanceof Number cz) {
                        fabricWorld.getChunk(cx.intValue(), cz.intValue());
                        return true;
                    }
                    if (methodName.equals("getEntities")) {
                        List<org.bukkit.entity.Entity> entities = new ArrayList<>();
                        for (net.minecraft.entity.Entity e : fabricWorld.iterateEntities()) {
                            entities.add(MythosEntity.wrap(e));
                        }
                        return entities;
                    }
                    if (methodName.equals("getLivingEntities")) {
                        List<org.bukkit.entity.LivingEntity> entities = new ArrayList<>();
                        for (net.minecraft.entity.Entity e : fabricWorld.iterateEntities()) {
                            if (e instanceof net.minecraft.entity.LivingEntity) {
                                org.bukkit.entity.Entity wrapped = MythosEntity.wrap(e);
                                if (wrapped instanceof org.bukkit.entity.LivingEntity le) {
                                    entities.add(le);
                                }
                            }
                        }
                        return entities;
                    }
                    if ((methodName.equals("dropItem") || methodName.equals("dropItemNaturally"))
                            && args != null && args.length >= 2
                            && args[0] instanceof org.bukkit.Location loc
                            && args[1] instanceof org.bukkit.inventory.ItemStack item) {
                        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                                fabricWorld, loc.getX(), loc.getY(), loc.getZ(), MythosItemStack.toFabric(item)
                        );
                        fabricWorld.spawnEntity(itemEntity);
                        return MythosEntity.wrap(itemEntity);
                    }
                    if (methodName.equals("equals")) {
                        Object other = args != null && args.length > 0 ? args[0] : null;
                        return other instanceof World w && worldUid.equals(w.getUID());
                    }
                    if (methodName.equals("hashCode")) return worldUid.hashCode();
                    if (methodName.equals("toString")) return "MythosWorld{" + bukkitName + "}";

                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class || returnType == Boolean.class) return false;
                    if (returnType == int.class || returnType == Integer.class) return 0;
                    if (returnType == long.class || returnType == Long.class) return 0L;
                    if (returnType == double.class || returnType == Double.class) return 0d;
                    if (returnType == float.class || returnType == Float.class) return 0f;
                    return null;
                }
        );
    }
}
