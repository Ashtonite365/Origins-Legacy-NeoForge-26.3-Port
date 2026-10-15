package io.github.apace100.origins.networking;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.OriginsClient;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.integration.OriginDataLoadedCallback;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModPacketsS2C {

    public static void register() {
        PlatformNetworking.registerClientReceiver(ModPackets.OPEN_ORIGIN_SCREEN, ModPacketsS2C::openOriginScreen);
        PlatformNetworking.registerClientReceiver(ModPackets.ORIGIN_LIST, ModPacketsS2C::receiveOriginList);
        PlatformNetworking.registerClientReceiver(ModPackets.LAYER_LIST, ModPacketsS2C::receiveLayerList);
        PlatformNetworking.registerClientReceiver(ModPackets.CONFIRM_ORIGIN, ModPacketsS2C::receiveOriginConfirmation);
        PlatformNetworking.registerClientReceiver(ModPackets.BADGE_LIST, ModPacketsS2C::receiveBadgeList);
        PlatformNetworking.registerClientReceiver(ModPackets.POWERS_AND_ORIGINS, (packet, ctx) -> {
            PowerTypeRegistry.clear();
            packet.powers().factories().forEach(PowerTypeRegistry::register);
            receiveOriginList(packet.origins(), ctx);
        });
    }

    private static void receiveOriginConfirmation(ConfirmOriginPacket packet, IPayloadContext context) {
        OriginLayer layer = OriginLayers.getLayer(packet.layerId());
        Origin origin = OriginRegistry.get(packet.originId());
        context.enqueueWork(() -> {
            OriginComponent component = ModComponents.ORIGIN.get(context.player());
            component.setOrigin(layer, origin);
            if(Minecraft.getInstance().gui.screen() instanceof WaitForNextLayerScreen layerScreen) {
                layerScreen.openSelection();
            }
        });
    }


    private static void openOriginScreen(OpenOriginScreenPacket packet, IPayloadContext context) {
        boolean showDirtBackground = packet.showDirtBackground();
        context.enqueueWork(() -> {
            ArrayList<OriginLayer> layers = new ArrayList<>();
            OriginComponent component = ModComponents.ORIGIN.get(context.player());
            OriginLayers.getLayers().forEach(layer -> {
                if(layer.isEnabled() && !component.hasOrigin(layer)) {
                    layers.add(layer);
                }
            });
            Collections.sort(layers);
            Minecraft.getInstance().gui.setScreen(new ChooseOriginScreen(layers, 0, showDirtBackground));
        });
    }

    private static void receiveOriginList(OriginListPacket packet, IPayloadContext context) {
        try {
            context.enqueueWork(() -> {
                OriginsClient.isServerRunningOrigins = true;
                OriginRegistry.reset();
                packet.origins().forEach(OriginRegistry::register);
            });
        } catch (Exception e) {
            Origins.LOGGER.error(e);
        }
    }

    private static void receiveLayerList(LayerListPacket packet, IPayloadContext context) {
        try {
            context.enqueueWork(() -> {
                OriginLayers.clear();
                packet.layers().forEach(OriginLayers::add);
                OriginDataLoadedCallback.EVENT.invoker().onDataLoaded(true);
            });
        } catch (Exception e) {
            Origins.LOGGER.error(e);
        }
    }

    private static void receiveBadgeList(BadgeListPacket packet, IPayloadContext context) {
        try {
            context.enqueueWork(() -> {
                BadgeManager.clear();
                for(Map.Entry<Identifier, List<Badge>> badgeEntry : packet.badges().entrySet()) {
                    for(Badge badge : badgeEntry.getValue()) {
                        BadgeManager.putPowerBadge(badgeEntry.getKey(), badge);
                    }
                }
            });
        } catch (Exception e) {
            Origins.LOGGER.error(e);
        }
    }
}
