package pl.mythoscore.entity;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MythosWorld {

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
