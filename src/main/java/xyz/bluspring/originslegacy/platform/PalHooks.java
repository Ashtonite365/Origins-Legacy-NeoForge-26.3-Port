/*
 * Origins: Legacy personal NeoForge port - platform layer. MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import io.github.ladysnake.pal.Pal;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Game-event hooks for H3's local PAL stand-in: forget grants on logout, re-apply on respawn / game-mode change. */
public final class PalHooks {
    private PalHooks() {}

    static void init() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> Pal.forget(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> Pal.refreshAll(e.getEntity()));
        // Fired before the mode is applied: re-apply on the next server task, after the vanilla abilities reset.
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangeGameModeEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                sp.level().getServer().execute(() -> Pal.refreshAll(sp));
            }
        });
    }
}
