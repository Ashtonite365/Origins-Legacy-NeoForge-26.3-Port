/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C component sync for entity {@code entityId}; {@code data} is what {@link PlatformComponent#writeSyncPacket} wrote. */
public record ComponentSyncPayload(int entityId, Identifier key, byte[] data) implements CustomPacketPayload {
    public static final Type<ComponentSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("origins", "component_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComponentSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ComponentSyncPayload::entityId,
        Identifier.STREAM_CODEC, ComponentSyncPayload::key,
        ByteBufCodecs.BYTE_ARRAY, ComponentSyncPayload::data,
        ComponentSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
