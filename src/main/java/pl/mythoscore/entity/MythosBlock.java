package pl.mythoscore.entity;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;

public class MythosBlock {

    public static Block wrap(ServerWorld world, BlockPos pos) {
        BlockPos immutablePos = pos.toImmutable();
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getType")) {
                        return MythosMaterials.fromFabricBlock(world.getBlockState(immutablePos).getBlock());
                    }
                    if (methodName.equals("setType") && args != null && args.length >= 1 && args[0] instanceof Material mat) {
                        world.setBlockState(immutablePos, MythosMaterials.toFabricBlock(mat).getDefaultState());
                        return null;
                    }
                    if (methodName.equals("getX")) return immutablePos.getX();
                    if (methodName.equals("getY")) return immutablePos.getY();
                    if (methodName.equals("getZ")) return immutablePos.getZ();
                    if (methodName.equals("getWorld")) return MythosWorld.wrap(world);
                    if (methodName.equals("getLocation")) {
                        return MythosLocation.wrap(world, immutablePos.getX(), immutablePos.getY(), immutablePos.getZ());
                    }
                    if (methodName.equals("getChunk")) {
                        return MythosChunk.wrap(world, immutablePos.getX() >> 4, immutablePos.getZ() >> 4);
                    }
                    if (methodName.equals("getState")) {
                        return MythosBlockState.wrap(world, immutablePos);
                    }
                    if (methodName.equals("getRelative") && args != null) {
                        if (args.length == 1 && args[0] instanceof BlockFace face) {
                            return wrap(world, immutablePos.add(face.getModX(), face.getModY(), face.getModZ()));
                        }
                        if (args.length == 2 && args[0] instanceof BlockFace face && args[1] instanceof Number dist) {
                            int d = dist.intValue();
                            return wrap(world, immutablePos.add(face.getModX() * d, face.getModY() * d, face.getModZ() * d));
                        }
                        if (args.length == 3 && args[0] instanceof Number dx && args[1] instanceof Number dy && args[2] instanceof Number dz) {
                            return wrap(world, immutablePos.add(dx.intValue(), dy.intValue(), dz.intValue()));
                        }
                    }
                    if (methodName.equals("isEmpty")) {
                        return world.getBlockState(immutablePos).isAir();
                    }
                    if (methodName.equals("isLiquid")) {
                        return !world.getFluidState(immutablePos).isEmpty();
                    }
                    if (methodName.equals("isPassable")) {
                        return world.getBlockState(immutablePos).getCollisionShape(world, immutablePos).isEmpty();
                    }
                    if (methodName.equals("getLightLevel")) {
                        return (byte) world.getLightLevel(immutablePos);
                    }
                    if (methodName.equals("breakNaturally")) {
                        return world.breakBlock(immutablePos, true);
                    }
                    if (methodName.equals("equals")) {
                        Object other = (args != null && args.length > 0) ? args[0] : null;
                        return other instanceof Block b
                                && b.getX() == immutablePos.getX()
                                && b.getY() == immutablePos.getY()
                                && b.getZ() == immutablePos.getZ()
                                && MythosWorld.toWorldUuid(world).equals(b.getWorld().getUID());
                    }
                    if (methodName.equals("hashCode")) {
                        return immutablePos.hashCode() * 31 + MythosWorld.toWorldUuid(world).hashCode();
                    }
                    if (methodName.equals("toString")) {
                        return "MythosBlock{" + immutablePos.getX() + "," + immutablePos.getY() + "," + immutablePos.getZ() + "}";
                    }

                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }
}
