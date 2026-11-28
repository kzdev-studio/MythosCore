package pl.mythoscore.server;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.mythoscore.entity.MythosEntity;

public final class MythosDamageListener {

    private MythosDamageListener() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity victim = MythosEntity.wrap(entity);
            if (victim == null) return true;

            EntityDamageEvent.DamageCause cause = toDamageCause(source);
            net.minecraft.entity.Entity attacker = source.getAttacker();

            EntityDamageEvent event;
            if (attacker != null) {
                Entity bukkitAttacker = MythosEntity.wrap(attacker);
                if (source.getSource() != attacker && cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    cause = EntityDamageEvent.DamageCause.PROJECTILE;
                }
                event = new EntityDamageByEntityEvent(bukkitAttacker, victim, cause, (double) amount);
            } else {
                event = new EntityDamageEvent(victim, cause, (double) amount);
            }

            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        });
    }

    private static EntityDamageEvent.DamageCause toDamageCause(DamageSource source) {
        if (source == null) return EntityDamageEvent.DamageCause.CUSTOM;
        if (source.isOf(DamageTypes.FALL)) return EntityDamageEvent.DamageCause.FALL;
        if (source.isOf(DamageTypes.DROWN)) return EntityDamageEvent.DamageCause.DROWNING;
        if (source.isOf(DamageTypes.IN_FIRE) || source.isOf(DamageTypes.ON_FIRE)) return EntityDamageEvent.DamageCause.FIRE;
        if (source.isOf(DamageTypes.LAVA)) return EntityDamageEvent.DamageCause.LAVA;
        if (source.isOf(DamageTypes.STARVE)) return EntityDamageEvent.DamageCause.STARVATION;
        if (source.isOf(DamageTypes.IN_WALL)) return EntityDamageEvent.DamageCause.SUFFOCATION;
        if (source.isOf(DamageTypes.OUT_OF_WORLD)) return EntityDamageEvent.DamageCause.VOID;
        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) return EntityDamageEvent.DamageCause.LIGHTNING;
        if (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) return EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        if (source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)) return EntityDamageEvent.DamageCause.MAGIC;
        if (source.isOf(DamageTypes.WITHER)) return EntityDamageEvent.DamageCause.WITHER;
        if (source.isOf(DamageTypes.FALLING_ANVIL) || source.isOf(DamageTypes.FALLING_BLOCK)) return EntityDamageEvent.DamageCause.FALLING_BLOCK;
        if (source.getAttacker() != null) return EntityDamageEvent.DamageCause.ENTITY_ATTACK;
        return EntityDamageEvent.DamageCause.CUSTOM;
    }
}
