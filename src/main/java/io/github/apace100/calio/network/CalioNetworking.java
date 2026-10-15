package io.github.apace100.calio.network;

import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class CalioNetworking {

    public static final CustomPacketPayload.Type<SyncDataObjectRegistryPacket<?>> SYNC_DATA_OBJECT_REGISTRY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("calio", "sync_data_object_registry"));

    public static void init() {
        PlatformNetworking.registerClientbound(SYNC_DATA_OBJECT_REGISTRY, SyncDataObjectRegistryPacket.CODEC);
    }
}
