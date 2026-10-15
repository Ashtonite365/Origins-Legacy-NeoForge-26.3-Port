package io.github.apace100.apoli.networking;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import net.minecraft.server.level.ServerPlayer;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class ModPacketsC2S {

    public static void register() {
        PlatformNetworking.registerServerReceiver(ModPackets.USE_ACTIVE_POWERS, ModPacketsC2S::useActivePowers);
        PlatformNetworking.registerServerReceiver(ModPackets.PLAYER_LANDED, ModPacketsC2S::playerLanded);
        PlatformNetworking.registerServerReceiver(ModPackets.PREVENTED_ENTITY_USE, ModPacketsC2S::interactionPrevented);
    }

    private static void playerLanded(PlayerLandedPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> PowerHolderComponent.getPowers(((ServerPlayer) context.player()), ActionOnLandPower.class).forEach(ActionOnLandPower::executeAction));
    }

    private static void interactionPrevented(PreventedEntityUsePacket payload, IPayloadContext context) {
        int otherEntityId = payload.otherEntityId();
        int handOrdinal = payload.handOrdinal();
        context.enqueueWork(() -> {
            Entity otherEntity = ((ServerPlayer) context.player()).level().getEntity(otherEntityId);
            InteractionHand hand = InteractionHand.values()[handOrdinal];
            if(otherEntity == null) {
                Apoli.LOGGER.warn("Received unknown entity for prevented interaction");
            } else {
                boolean prevented = false;
                for(PreventEntityUsePower peup : PowerHolderComponent.getPowers(((ServerPlayer) context.player()), PreventEntityUsePower.class)) {
                    if(peup.doesApply(otherEntity, hand, ((ServerPlayer) context.player()).getItemInHand(hand))) {
                        peup.executeAction(otherEntity, hand);
                        prevented = true;
                        break;
                    }
                }
                if(!prevented) {
                    for(PreventBeingUsedPower pbup : PowerHolderComponent.getPowers(otherEntity, PreventBeingUsedPower.class)) {
                        if(pbup.doesApply(((ServerPlayer) context.player()), hand, ((ServerPlayer) context.player()).getItemInHand(hand))) {
                            pbup.executeAction(((ServerPlayer) context.player()), hand);
                            prevented = true;
                            break;
                        }
                    }
                    if(!prevented) {
                        Apoli.LOGGER.warn("Couldn't find corresponding entity use preventing power");
                    }
                }
            }
        });
    }

    private static void useActivePowers(UseActivePowersPacket payload, IPayloadContext context) {
        var powerTypes = payload.powers();
        context.enqueueWork(() -> {
            PowerHolderComponent component = PowerHolderComponent.KEY.get(((ServerPlayer) context.player()));
            for(PowerType<?> type : powerTypes) {
                Power power = component.getPower(type);
                if(power instanceof Active) {
                    ((Active) power).onUse();
                }
            }
        });
    }


}
