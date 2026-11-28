package pl.mythoscore.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;
import java.util.UUID;

public class MythosEntity {

    public static Entity wrap(net.minecraft.entity.Entity fabricEntity) {
        if (fabricEntity == null) {
            return null;
        }
        if (fabricEntity instanceof ServerPlayerEntity sp) {
            return MythosPlayer.wrap(sp);
        }

        Class<?>[] interfaces = (fabricEntity instanceof LivingEntity)
                ? new Class<?>[]{org.bukkit.entity.LivingEntity.class}
                : new Class<?>[]{Entity.class};

        UUID uid = fabricEntity.getUuid();
        EntityType entityType = MythosMaterials.toBukkitEntityType(fabricEntity.getType());

        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(),
                interfaces,
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getUniqueId")) return uid;
                    if (methodName.equals("getEntityId")) return fabricEntity.getId();
                    if (methodName.equals("getType")) return entityType;
                    if (methodName.equals("getName")) return fabricEntity.getName().getString();
                    if (methodName.equals("getCustomName")) {
                        Text custom = fabricEntity.getCustomName();
                        return custom != null ? custom.getString() : null;
                    }
                    if (methodName.equals("setCustomName") && args != null && args.length > 0) {
                        String name = (String) args[0];
                        fabricEntity.setCustomName(name != null ? Text.literal(name) : null);
                        return null;
                    }
                    if (methodName.equals("isCustomNameVisible")) return fabricEntity.isCustomNameVisible();
                    if (methodName.equals("setCustomNameVisible") && args != null && args.length > 0 && args[0] instanceof Boolean b) {
                        fabricEntity.setCustomNameVisible(b);
                        return null;
                    }
                    if (methodName.equals("getServer")) return Bukkit.getServer();
                    if (methodName.equals("getWorld")) {
                        if (fabricEntity.getWorld() instanceof ServerWorld sw) {
                            return MythosWorld.wrap(sw);
                        }
                        return null;
                    }
                    if (methodName.equals("getLocation")) {
                        if (fabricEntity.getWorld() instanceof ServerWorld sw) {
                            return MythosLocation.wrap(sw, fabricEntity.getX(), fabricEntity.getY(), fabricEntity.getZ(),
                                    fabricEntity.getYaw(), fabricEntity.getPitch());
                        }
                        return new Location(null, fabricEntity.getX(), fabricEntity.getY(), fabricEntity.getZ(),
                                fabricEntity.getYaw(), fabricEntity.getPitch());
                    }
                    if (methodName.equals("teleport") && args != null && args.length > 0) {
                        Location loc = null;
                        if (args[0] instanceof Location l) loc = l;
                        else if (args[0] instanceof Entity ent) loc = ent.getLocation();
                        if (loc != null) {
                            ServerWorld targetWorld = MythosWorld.getFabricWorld(loc.getWorld());
                            if (targetWorld != null && fabricEntity.getWorld() instanceof ServerWorld currentWorld && targetWorld != currentWorld) {
                                fabricEntity.moveToWorld(targetWorld);
                            }
                            fabricEntity.refreshPositionAndAngles(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                            return true;
                        }
                        return false;
                    }
                    if (methodName.equals("isDead")) return !fabricEntity.isAlive();
                    if (methodName.equals("isValid")) return fabricEntity.isAlive() && !fabricEntity.isRemoved();
                    if (methodName.equals("remove")) {
                        fabricEntity.discard();
                        return null;
                    }
                    if (methodName.equals("isOnGround")) return fabricEntity.isOnGround();
                    if (methodName.equals("getVelocity")) {
                        Vec3d v = fabricEntity.getVelocity();
                        return new Vector(v.x, v.y, v.z);
                    }
                    if (methodName.equals("setVelocity") && args != null && args.length > 0 && args[0] instanceof Vector v) {
                        fabricEntity.setVelocity(v.getX(), v.getY(), v.getZ());
                        fabricEntity.velocityModified = true;
                        return null;
                    }

                    if (fabricEntity instanceof LivingEntity living) {
                        if (methodName.equals("getHealth")) return (double) living.getHealth();
                        if (methodName.equals("setHealth") && args != null && args.length > 0 && args[0] instanceof Number n) {
                            living.setHealth(n.floatValue());
                            return null;
                        }
                        if (methodName.equals("getMaxHealth")) return (double) living.getMaxHealth();
                        if (methodName.equals("getEyeLocation") && fabricEntity.getWorld() instanceof ServerWorld sw) {
                            return MythosLocation.wrap(sw, living.getX(), living.getEyeY(), living.getZ(),
                                    living.getYaw(), living.getPitch());
                        }
                        if (methodName.equals("damage") && args != null && args.length >= 1 && args[0] instanceof Number n) {
                            living.damage(living.getDamageSources().generic(), n.floatValue());
                            return null;
                        }
                    }

                    if (methodName.equals("equals")) {
                        Object other = (args != null && args.length > 0) ? args[0] : null;
                        return other instanceof Entity e && uid.equals(e.getUniqueId());
                    }
                    if (methodName.equals("hashCode")) return uid.hashCode();
                    if (methodName.equals("toString")) return "MythosEntity{" + entityType + "," + uid + "}";

                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }
}
