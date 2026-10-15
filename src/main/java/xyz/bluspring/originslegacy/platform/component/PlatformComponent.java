/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A per-entity data component (CCA Component + AutoSyncedComponent). Saved through a NeoForge attachment,
 * synced to clients with {@link ComponentKey#sync(Object)}.
 */
public interface PlatformComponent {
    void readData(ValueInput input);

    void writeData(ValueOutput output);

    /** Whether {@code player} should receive this component's sync packets (CCA AutoSyncedComponent#shouldSyncWith). */
    default boolean shouldSyncWith(ServerPlayer player) { return true; }

    /** Default sync: full NBT of {@link #writeData}. */
    default void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, buf.registryAccess());
        writeData(out);
        buf.writeNbt(out.buildResult());
    }

    default void applySyncPacket(RegistryFriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag != null) readData(TagValueInput.create(ProblemReporter.DISCARDING, buf.registryAccess(), tag));
    }
}
