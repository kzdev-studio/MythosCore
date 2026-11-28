package pl.mythoscore.entity;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;

public class MythosBlockState {

    public static BlockState wrap(ServerWorld world, BlockPos pos) {
        BlockPos immutablePos = pos.toImmutable();
        final Material[] snapshotType = new Material[]{
                MythosMaterials.fromFabricBlock(world.getBlockState(immutablePos).getBlock())
        };

        return (BlockState) Proxy.newProxyInstance(
                BlockState.class.getClassLoader(),
                new Class<?>[]{BlockState.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getType")) return snapshotType[0];
                    if (methodName.equals("setType") && args != null && args.length > 0 && args[0] instanceof Material mat) {
                        snapshotType[0] = mat;
                        return null;
                    }
                    if (methodName.equals("getBlock")) return MythosBlock.wrap(world, immutablePos);
                    if (methodName.equals("getWorld")) return MythosWorld.wrap(world);
                    if (methodName.equals("getLocation")) {
                        return MythosLocation.wrap(world, immutablePos.getX(), immutablePos.getY(), immutablePos.getZ());
                    }
                    if (methodName.equals("getX")) return immutablePos.getX();
                    if (methodName.equals("getY")) return immutablePos.getY();
                    if (methodName.equals("getZ")) return immutablePos.getZ();
                    if (methodName.equals("getChunk")) {
                        return MythosChunk.wrap(world, immutablePos.getX() >> 4, immutablePos.getZ() >> 4);
                    }
                    if (methodName.equals("update")) {
                        world.setBlockState(immutablePos, MythosMaterials.toFabricBlock(snapshotType[0]).getDefaultState());
                        return true;
                    }
                    if (methodName.equals("isPlaced")) return true;

                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }
}
