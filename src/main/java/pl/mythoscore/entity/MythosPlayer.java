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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.server.MythosServer;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MythosPlayer {

    public static final Map<String, ServerPlayerEntity> PLAYER_MAP = new ConcurrentHashMap<>();

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
                                fabricPlayer.getX(), fabricPlayer.getY(), fabricPlayer.getZ()
                        );
                    }
                    if (methodName.equals("teleport")) {
                        Location loc = (Location) args[0];
                        fabricPlayer.teleport(
                                (net.minecraft.server.world.ServerWorld) fabricPlayer.getServer().getOverworld(),
                                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
                        );
                        return true;
                    }

                    if (methodName.equals("openInventory")) {
                        org.bukkit.inventory.Inventory bukkitInv = (org.bukkit.inventory.Inventory) args[0];
                        MythosInventory customInv = MythosInventory.INVENTORY_MAP.get(bukkitInv);
                        if (customInv != null) customInv.openFor(fabricPlayer);
                        return null;
                    }

                    if (methodName.equals("hasPermission")) {
                        String permission = (String) args[0];
                        return MythosCoreMod.permissionManager.hasPermission(fabricPlayer.getName().getString(), permission);
                    }

                    if (methodName.equals("sendMessage")) {
                        if (args != null && args.length > 0 && args[0] instanceof String) {
                            fabricPlayer.sendMessage(Text.literal((String) args[0]), false);
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
}