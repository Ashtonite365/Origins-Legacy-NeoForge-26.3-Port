package io.github.apace100.calio.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;


public class CalioNetworkingClient {

    public static void registerReceivers() {
        PlatformNetworking.registerClientReceiver(
            CalioNetworking.SYNC_DATA_OBJECT_REGISTRY,
            CalioNetworkingClient::onDataObjectRegistrySync
        );
    }

    private static void onDataObjectRegistrySync(
        SyncDataObjectRegistryPacket<?> packet,
        IPayloadContext context) {
        //DataObjectRegistry.getRegistry(packet.registryId()).receive(packet.entries(),
            //context.client().hasSingleplayerServer() ? r -> {} : context.client()::execute);
        /*minecraftClient.execute(() -> {
            DataObjectRegistry.getRegistry(registryId).receive(packetByteBuf);
        });*/
    }
}
