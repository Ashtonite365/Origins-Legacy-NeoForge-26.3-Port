/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;

/** Component ticking and automatic sync (login, start tracking, respawn, dimension change). */
public final class PlatformComponents {
    private PlatformComponents() {}

    public static void init(IEventBus modBus) {
        PlatformNetworking.registerClientbound(ComponentSyncPayload.TYPE, ComponentSyncPayload.CODEC);
        // Runs only on the client, but uses no client-only classes.
        PlatformNetworking.registerClientReceiver(ComponentSyncPayload.TYPE, (payload, ctx) -> {
            Entity entity = ctx.player().level().getEntity(payload.entityId());
            if (entity != null) ComponentKey.byId(payload.key()).ifPresent(k -> k.applySync(entity, payload.data()));
        });

        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, PlatformComponents::onTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> syncSelf(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> syncAll(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> syncAll(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                for (ComponentKey<?> key : ComponentKey.all()) {
                    if (key.isPresent(e.getTarget())) key.syncWith(sp, e.getTarget());
                }
            }
        });
    }

    private static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        boolean client = entity.level().isClientSide();
        for (ComponentKey<?> key : ComponentKey.all()) {
            if (!key.isPresent(entity)) continue;
            Object c = key.get(entity);
            if (!client && c instanceof ServerTickingComponent t) t.serverTick();
            else if (client && c instanceof ClientTickingComponent t) t.clientTick();
        }
    }

    private static void syncSelf(Entity player) {
        if (player instanceof ServerPlayer sp) {
            for (ComponentKey<?> key : ComponentKey.all()) if (key.isProvidedBy(sp)) key.syncWith(sp, sp);
        }
    }

    private static void syncAll(Entity player) {
        for (ComponentKey<?> key : ComponentKey.all()) if (key.isPresent(player)) key.sync(player);
    }
}
