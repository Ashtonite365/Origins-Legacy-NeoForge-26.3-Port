package io.github.apace100.origins.networking;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Random;

public class ModPacketsC2S {

    public static void register() {
        // Version check: NeoForge payload channel versioning replaces the Fabric login handshake.
        PlatformNetworking.registerServerReceiver(ModPackets.CHOOSE_ORIGIN, ModPacketsC2S::chooseOrigin);
        PlatformNetworking.registerServerReceiver(ModPackets.CHOOSE_RANDOM_ORIGIN, ModPacketsC2S::chooseRandomOrigin);
    }

    private static void chooseOrigin(ChooseOriginPacket packet, IPayloadContext context) {
        Identifier originId = packet.originId();
        Identifier layerId = packet.layerId();
        ServerPlayer playerEntity = ((ServerPlayer) context.player());
        context.enqueueWork(() -> {
            OriginComponent component = ModComponents.ORIGIN.get(playerEntity);
            OriginLayer layer = OriginLayers.getLayer(layerId);
            if(!component.hasAllOrigins() && !component.hasOrigin(layer)) {
                if(originId != null) {
                    Origin origin = OriginRegistry.get(originId);
                    if(origin.isChoosable() && layer.contains(origin, playerEntity)) {
                        boolean hadOriginBefore = component.hadOriginBefore();
                        boolean hadAllOrigins = component.hasAllOrigins();
                        component.setOrigin(layer, origin);
                        component.checkAutoChoosingLayers(playerEntity, false);
                        component.sync();
                        if(component.hasAllOrigins() && !hadAllOrigins) {
                            OriginComponent.onChosen(playerEntity, hadOriginBefore);
                        }
                        Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " chose Origin: " + originId + ", for layer: " + layerId);
                    } else {
                        Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " tried to choose unchoosable Origin for layer " + layerId + ": " + originId + ".");
                        component.setOrigin(layer, Origin.EMPTY);
                    }
                    confirmOrigin(playerEntity, layer, component.getOrigin(layer));
                    component.sync();
                } else {
                    Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " chose unknown origin: " + originId);
                }
            } else {
                Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " tried to choose origin for layer " + layerId + " while having one already.");
            }
        });
    }

    private static void chooseRandomOrigin(ChooseRandomOriginPacket packet, IPayloadContext context) {
        Identifier layerId = packet.layerId();
        ServerPlayer playerEntity = ((ServerPlayer) context.player());
        context.enqueueWork(() -> {
            OriginComponent component = ModComponents.ORIGIN.get(playerEntity);
            OriginLayer layer = OriginLayers.getLayer(layerId);
            if(!component.hasAllOrigins() && !component.hasOrigin(layer)) {
                List<Identifier> randomOrigins = layer.getRandomOrigins(playerEntity);
                if(layer.isRandomAllowed() && randomOrigins.size() > 0) {
                    Identifier randomOrigin = randomOrigins.get(new Random().nextInt(randomOrigins.size()));
                    Origin origin = OriginRegistry.get(randomOrigin);
                    boolean hadOriginBefore = component.hadOriginBefore();
                    boolean hadAllOrigins = component.hasAllOrigins();
                    component.setOrigin(layer, origin);
                    component.checkAutoChoosingLayers(playerEntity, false);
                    component.sync();
                    if(component.hasAllOrigins() && !hadAllOrigins) {
                        OriginComponent.onChosen(playerEntity, hadOriginBefore);
                    }
                    Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " was randomly assigned the following Origin: " + randomOrigin + ", for layer: " + layerId);
                } else {
                    Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " tried to choose a random Origin for layer " + layerId + ", which is not allowed!");
                    component.setOrigin(layer, Origin.EMPTY);
                }
                confirmOrigin(playerEntity, layer, component.getOrigin(layer));
                component.sync();
            } else {
                Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " tried to choose origin for layer " + layerId + " while having one already.");
            }
        });
    }



    private static void confirmOrigin(ServerPlayer player, OriginLayer layer, Origin origin) {
        PlatformNetworking.sendToPlayer(player, new ConfirmOriginPacket(layer.getIdentifier(), origin.getIdentifier()));
    }
}
