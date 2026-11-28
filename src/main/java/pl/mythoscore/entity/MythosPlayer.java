package pl.mythoscore.entity;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

public class MythosPlayer {

    public static final Map<String, ServerPlayerEntity> PLAYER_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, PermissibleBase> PERMISSIBLES = new ConcurrentHashMap<>();

    public static Player wrap(ServerPlayerEntity fabricPlayer) {
        Player bukkitPlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getName")) return fabricPlayer.getName().getString();
                    if (methodName.equals("getUniqueId") || methodName.equals("getUUID")) return fabricPlayer.getUuid();
                    if (methodName.equals("isOnline")) return true;
                    if (methodName.equals("isOp")) {
                        return MythosCoreMod.MC_SERVER != null
                                && MythosCoreMod.MC_SERVER.getPlayerManager().isOperator(fabricPlayer.getGameProfile());
                    }
                    if (methodName.equals("setOp") && args != null && args.length > 0 && args[0] instanceof Boolean op) {
                        if (MythosCoreMod.MC_SERVER != null) {
                            if (op) {
                                MythosCoreMod.MC_SERVER.getPlayerManager().addToOperators(fabricPlayer.getGameProfile());
                            } else {
                                MythosCoreMod.MC_SERVER.getPlayerManager().removeFromOperators(fabricPlayer.getGameProfile());
                            }
                        }
                        return null;
                    }
                    if (methodName.equals("getGameMode")) {
                        return toBukkitGameMode(fabricPlayer.interactionManager.getGameMode());
                    }
                    if (methodName.equals("setGameMode") && args != null && args.length > 0 && args[0] instanceof org.bukkit.GameMode gm) {
                        fabricPlayer.changeGameMode(toFabricGameMode(gm));
                        return null;
                    }
                    if (methodName.equals("equals")) {
                        Object other = (args != null && args.length > 0) ? args[0] : null;
                        if (proxy == other) return true;
                        if (other instanceof Player otherPlayer) {
                            return fabricPlayer.getName().getString().equalsIgnoreCase(otherPlayer.getName());
                        }
                        return false;
                    }
                    if (methodName.equals("hashCode")) {
                        return fabricPlayer.getName().getString().toLowerCase().hashCode();
                    }
                    if (methodName.equals("toString")) {
                        return "MythosPlayer{" + fabricPlayer.getName().getString() + "}";
                    }
                    // Player.getServer() MUSI zwracac serwer - inaczej NPE w kazdym pluginie,
                    // ktory robi player.getServer().getScoreboardManager() (np. nametagi).
                    // UWAGA: fabricPlayer.getServer() to MinecraftServer (class_3176), NIE org.bukkit.Server!
                    // Dlatego ZAWSZE idziemy przez Bukkit.getServer() (nasze proxy).
                    if (methodName.equals("getServer")) {
                        try {
                            return org.bukkit.Bukkit.getServer();
                        } catch (Throwable ignored) {
                            return null;
                        }
                    }
                    if (methodName.equals("getDisplayName")) return fabricPlayer.getName().getString();
                    if (methodName.equals("getPlayerListName")) return fabricPlayer.getName().getString();

                    if (methodName.equals("getWorld")) {
                        return MythosWorld.wrap((net.minecraft.server.world.ServerWorld) fabricPlayer.getWorld());
                    }
                    if (methodName.equals("getLocation")) {
                        return MythosLocation.wrap(
                                (net.minecraft.server.world.ServerWorld) fabricPlayer.getWorld(),
                                fabricPlayer.getX(), fabricPlayer.getY(), fabricPlayer.getZ(),
                                fabricPlayer.getYaw(), fabricPlayer.getPitch()
                        );
                    }
                    if (methodName.equals("getEyeLocation")) {
                        return MythosLocation.wrap(
                                (net.minecraft.server.world.ServerWorld) fabricPlayer.getWorld(),
                                fabricPlayer.getX(), fabricPlayer.getY() + fabricPlayer.getStandingEyeHeight(), fabricPlayer.getZ(),
                                fabricPlayer.getYaw(), fabricPlayer.getPitch()
                        );
                    }
                    if (methodName.equals("teleport") && args != null && args.length > 0) {
                        Location loc = null;
                        if (args[0] instanceof Location l) {
                            loc = l;
                        } else if (args[0] instanceof Entity ent) {
                            loc = ent.getLocation();
                        }
                        if (loc != null) {
                            net.minecraft.server.world.ServerWorld targetWorld = MythosWorld.getFabricWorld(loc.getWorld());
                            if (targetWorld == null) {
                                targetWorld = fabricPlayer.getServerWorld();
                            }
                            fabricPlayer.teleport(
                                    targetWorld,
                                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
                            );
                            return true;
                        }
                        return false;
                    }
                    if (methodName.equals("getEntityId")) return fabricPlayer.getId();
                    if (methodName.equals("isDead")) return fabricPlayer.isDead();
                    if (methodName.equals("getHealth")) return (double) fabricPlayer.getHealth();
                    if (methodName.equals("setHealth") && args != null && args.length > 0 && args[0] instanceof Number n) {
                        fabricPlayer.setHealth(n.floatValue());
                        return null;
                    }
                    if (methodName.equals("getMaxHealth")) return (double) fabricPlayer.getMaxHealth();
                    if (methodName.equals("getFoodLevel")) return fabricPlayer.getHungerManager().getFoodLevel();
                    if (methodName.equals("setFoodLevel") && args != null && args.length > 0 && args[0] instanceof Number n) {
                        fabricPlayer.getHungerManager().setFoodLevel(n.intValue());
                        return null;
                    }
                    if (methodName.equals("getSaturation")) return fabricPlayer.getHungerManager().getSaturationLevel();
                    if (methodName.equals("setSaturation") && args != null && args.length > 0 && args[0] instanceof Number n) {
                        fabricPlayer.getHungerManager().setSaturationLevel(n.floatValue());
                        return null;
                    }
                    if (methodName.equals("isSneaking")) return fabricPlayer.isSneaking();
                    if (methodName.equals("setSneaking") && args != null && args.length > 0 && args[0] instanceof Boolean b) {
                        fabricPlayer.setSneaking(b);
                        return null;
                    }
                    if (methodName.equals("isSprinting")) return fabricPlayer.isSprinting();
                    if (methodName.equals("setSprinting") && args != null && args.length > 0 && args[0] instanceof Boolean b) {
                        fabricPlayer.setSprinting(b);
                        return null;
                    }
                    if (methodName.equals("getAllowFlight")) return fabricPlayer.getAbilities().allowFlying;
                    if (methodName.equals("setAllowFlight") && args != null && args.length > 0 && args[0] instanceof Boolean b) {
                        fabricPlayer.getAbilities().allowFlying = b;
                        if (!b) fabricPlayer.getAbilities().flying = false;
                        fabricPlayer.sendAbilitiesUpdate();
                        return null;
                    }
                    if (methodName.equals("isFlying")) return fabricPlayer.getAbilities().flying;
                    if (methodName.equals("setFlying") && args != null && args.length > 0 && args[0] instanceof Boolean b) {
                        fabricPlayer.getAbilities().flying = b;
                        fabricPlayer.sendAbilitiesUpdate();
                        return null;
                    }
                    if (methodName.equals("setFlySpeed") && args != null && args.length > 0 && args[0] instanceof Number n) {
                        fabricPlayer.getAbilities().setFlySpeed(n.floatValue() / 2.0f);
                        fabricPlayer.sendAbilitiesUpdate();
                        return null;
                    }
                    if (methodName.equals("setWalkSpeed") && args != null && args.length > 0 && args[0] instanceof Number n) {
                        fabricPlayer.getAbilities().setWalkSpeed(n.floatValue() / 2.0f);
                        fabricPlayer.sendAbilitiesUpdate();
                        return null;
                    }

                    if (methodName.equals("openInventory")) {
                        org.bukkit.inventory.Inventory bukkitInv = (org.bukkit.inventory.Inventory) args[0];
                        MythosInventory customInv = MythosInventory.INVENTORY_MAP.get(bukkitInv);
                        if (customInv != null) customInv.openFor(fabricPlayer);
                        return null;
                    }

                    if (methodName.equals("hasPermission")) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        if (args != null && args.length > 0) {
                            if (args[0] instanceof String permName) {
                                if (permBase.isPermissionSet(permName)) {
                                    return permBase.hasPermission(permName);
                                }
                                return permBase.hasPermission(permName)
                                        || MythosCoreMod.permissionManager.hasPermission(fabricPlayer.getName().getString(), permName);
                            }
                            if (args[0] instanceof Permission perm) {
                                if (permBase.isPermissionSet(perm)) {
                                    return permBase.hasPermission(perm);
                                }
                                return permBase.hasPermission(perm)
                                        || MythosCoreMod.permissionManager.hasPermission(fabricPlayer.getName().getString(), perm.getName());
                            }
                        }
                        return false;
                    }
                    if (methodName.equals("isPermissionSet") && args != null && args.length > 0) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        if (args[0] instanceof String s) return permBase.isPermissionSet(s);
                        if (args[0] instanceof Permission p) return permBase.isPermissionSet(p);
                    }
                    if (methodName.equals("addAttachment") && args != null) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        if (args.length == 1 && args[0] instanceof Plugin pl) return permBase.addAttachment(pl);
                        if (args.length == 2 && args[0] instanceof Plugin pl && args[1] instanceof Number ticks) {
                            return permBase.addAttachment(pl, ticks.intValue());
                        }
                        if (args.length == 3 && args[0] instanceof Plugin pl && args[1] instanceof String name && args[2] instanceof Boolean val) {
                            return permBase.addAttachment(pl, name, val);
                        }
                        if (args.length == 4 && args[0] instanceof Plugin pl && args[1] instanceof String name && args[2] instanceof Boolean val && args[3] instanceof Number ticks) {
                            return permBase.addAttachment(pl, name, val, ticks.intValue());
                        }
                    }
                    if (methodName.equals("removeAttachment") && args != null && args.length == 1 && args[0] instanceof PermissionAttachment att) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        permBase.removeAttachment(att);
                        return null;
                    }
                    if (methodName.equals("recalculatePermissions")) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        permBase.recalculatePermissions();
                        return null;
                    }
                    if (methodName.equals("getEffectivePermissions")) {
                        PermissibleBase permBase = PERMISSIBLES.computeIfAbsent(fabricPlayer.getUuid(), u -> new PermissibleBase((Player) proxy));
                        return permBase.getEffectivePermissions();
                    }

                    if (methodName.equals("sendMessage")) {
                        if (args != null && args.length > 0) {
                            if (args[0] instanceof String msg) {
                                fabricPlayer.sendMessage(Text.literal(msg), false);
                            } else if (args[0] instanceof String[] msgs) {
                                for (String msg : msgs) {
                                    if (msg != null) fabricPlayer.sendMessage(Text.literal(msg), false);
                                }
                            }
                        }
                        return null;
                    }

                    if (methodName.equals("kickPlayer")) {
                        if (args != null && args.length > 0 && args[0] instanceof String) {
                            fabricPlayer.networkHandler.disconnect(Text.literal((String) args[0]));
                        }
                        return null;
                    }

                    // --- DŹWIĘKI ---
                    if (methodName.equals("playSound")) {
                        if (args != null && args.length >= 4) {
                            Location loc = (Location) args[0];
                            Object soundObj = args[1];
                            float volume = (float) args[2];
                            float pitch = (float) args[3];

                            String soundId = "";
                            if (soundObj instanceof Sound) {
                                soundId = ((Sound) soundObj).getKey().toString();
                            } else if (soundObj instanceof String) {
                                soundId = (String) soundObj;
                            }

                            if (!soundId.isEmpty()) {
                                try {
                                    Identifier id = Identifier.tryParse(soundId);
                                    if (id != null) {
                                        SoundEvent soundEvent = SoundEvent.of(id);
                                        RegistryEntry<SoundEvent> entry = RegistryEntry.of(soundEvent);
                                        fabricPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(
                                                entry, SoundCategory.MASTER,
                                                loc.getX(), loc.getY(), loc.getZ(),
                                                volume, pitch, fabricPlayer.getRandom().nextLong()
                                        ));
                                    }
                                } catch (Exception e) {
                                    MythosCoreMod.LOGGER.error("Błąd dźwięku: " + soundId, e);
                                }
                            }
                        }
                        return null;
                    }

                    // --- EFEKTY ---
                    if (methodName.equals("addPotionEffect")) {
                        if (args != null && args.length > 0 && args[0] instanceof PotionEffect) {
                            PotionEffect effect = (PotionEffect) args[0];
                            Identifier id = Identifier.tryParse(effect.getType().getKey().toString());
                            if (id != null) {
                                StatusEffect statusEffect = Registries.STATUS_EFFECT.get(id);
                                if (statusEffect != null) {
                                    fabricPlayer.addStatusEffect(new StatusEffectInstance(
                                            statusEffect, effect.getDuration(), effect.getAmplifier(),
                                            effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
                                    ));
                                    return true;
                                }
                            }
                        }
                        return false;
                    }

                    if (methodName.equals("removePotionEffect")) {
                        if (args != null && args.length > 0 && args[0] instanceof PotionEffectType) {
                            PotionEffectType type = (PotionEffectType) args[0];
                            Identifier id = Identifier.tryParse(type.getKey().toString());
                            if (id != null) {
                                StatusEffect statusEffect = Registries.STATUS_EFFECT.get(id);
                                if (statusEffect != null) {
                                    fabricPlayer.removeStatusEffect(statusEffect);
                                }
                            }
                        }
                        return null;
                    }

                    // --- PARTICLES ---
                    if (methodName.equals("spawnParticle")) {
                        if (args != null && args.length >= 7) {
                            Object particleObj = args[0];
                            Location loc = (Location) args[1];
                            int count = (int) args[2];
                            double offX = (double) args[3];
                            double offY = (double) args[4];
                            double offZ = (double) args[5];
                            double extra = (double) args[6];

                            String particleName = "";
                            if (particleObj instanceof Particle) {
                                // POPRAWKA: Używamy toString().toLowerCase() zamiast getKey()
                                particleName = particleObj.toString().toLowerCase();
                            }

                            if (!particleName.isEmpty()) {
                                try {
                                    Identifier id = Identifier.tryParse(particleName);
                                    if (id != null) {
                                        ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
                                        if (type instanceof ParticleEffect effect) {
                                            fabricPlayer.networkHandler.sendPacket(new ParticleS2CPacket(
                                                    effect, false,
                                                    loc.getX(), loc.getY(), loc.getZ(),
                                                    (float) offX, (float) offY, (float) offZ,
                                                    (float) extra, count
                                            ));
                                        }
                                    }
                                } catch (Exception e) {
                                    MythosCoreMod.LOGGER.error("Błąd particli: " + particleName, e);
                                }
                            }
                        }
                        return null;
                    }

                    if (methodName.equals("sendTitle")) {
                        String title = args[0] != null ? (String) args[0] : "";
                        String subtitle = args[1] != null ? (String) args[1] : "";
                        int fadeIn = (int) args[2];
                        int stay = (int) args[3];
                        int fadeOut = (int) args[4];

                        fabricPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(fadeIn, stay, fadeOut));
                        if (!title.isEmpty()) fabricPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal(title)));
                        if (!subtitle.isEmpty()) fabricPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.literal(subtitle)));
                        return null;
                    }

                    if (methodName.equals("sendActionBar")) {
                        String message = args[0] != null ? (String) args[0] : "";
                        fabricPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(Text.literal(message)));
                        return null;
                    }

                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );

        String playerName = fabricPlayer.getName().getString();
        PLAYER_MAP.put(playerName.toLowerCase(), fabricPlayer);
        return bukkitPlayer;
    }

    public static ServerPlayerEntity getFabricPlayer(Player bukkitPlayer) {
        if (bukkitPlayer == null) return null;
        ServerPlayerEntity sp = PLAYER_MAP.get(bukkitPlayer.getName().toLowerCase());
        if (sp != null && !sp.isRemoved()) return sp;
        if (MythosCoreMod.MC_SERVER != null) {
            for (ServerPlayerEntity online : MythosCoreMod.MC_SERVER.getPlayerManager().getPlayerList()) {
                if (online.getName().getString().equalsIgnoreCase(bukkitPlayer.getName())) {
                    return online;
                }
            }
        }
        return null;
    }

    private static org.bukkit.GameMode toBukkitGameMode(GameMode mcMode) {
        if (mcMode == null) return org.bukkit.GameMode.SURVIVAL;
        return switch (mcMode) {
            case CREATIVE -> org.bukkit.GameMode.CREATIVE;
            case ADVENTURE -> org.bukkit.GameMode.ADVENTURE;
            case SPECTATOR -> org.bukkit.GameMode.SPECTATOR;
            default -> org.bukkit.GameMode.SURVIVAL;
        };
    }

    private static GameMode toFabricGameMode(org.bukkit.GameMode bukkitMode) {
        if (bukkitMode == null) return GameMode.SURVIVAL;
        return switch (bukkitMode) {
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }
}