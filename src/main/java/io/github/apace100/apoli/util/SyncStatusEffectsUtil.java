package io.github.apace100.apoli.util;

import io.github.apace100.apoli.networking.SyncStatusEffectPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public class SyncStatusEffectsUtil {

    public static void sendStatusEffectUpdatePacket(LivingEntity living, UpdateType type, MobEffectInstance instance) {
        if (living.level().isClientSide()) return;

        // Fabric PlayerLookup.tracking excluded the entity itself; sendToPlayersTrackingEntity does the same.
        PacketDistributor.sendToPlayersTrackingEntity(living, new SyncStatusEffectPacket(living.getId(), (byte) type.ordinal(), type == UpdateType.CLEAR ? Optional.empty() : Optional.of(instance)));
    }

    public enum UpdateType {
        CLEAR, APPLY, UPGRADE, REMOVE
    }
}