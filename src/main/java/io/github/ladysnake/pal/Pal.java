/*
 * Minimal local stand-in for PlayerAbilityLib (io.github.ladysnake.pal) used by the personal NeoForge port of
 * Origins: Legacy. Written from scratch against PAL's public API names only; no PAL (LGPL-3.0) code is copied.
 * Part of the Origins: Legacy port, MIT licensed (see LICENSE-apoli).
 */
package io.github.ladysnake.pal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry + per-player grant tracking. Grants live in memory only (keyed by player UUID, separate for each
 * logical side); Apoli's PlayerAbilityPower re-grants every tick while active, so nothing needs saving.
 * Abilities sync to the client through vanilla ServerPlayer#onUpdateAbilities.
 */
public final class Pal {
    private Pal() {}
    private static final Map<Identifier, PlayerAbility> ABILITIES = new ConcurrentHashMap<>();
    private static final Map<Identifier, AbilitySource> SOURCES = new ConcurrentHashMap<>();
    private static final Map<Boolean, Map<UUID, Map<PlayerAbility, Set<AbilitySource>>>> GRANTS = new ConcurrentHashMap<>();

    static PlayerAbility registerAbility(PlayerAbility ability) { ABILITIES.put(ability.getId(), ability); return ability; }

    public static AbilitySource getAbilitySource(Identifier id) { return SOURCES.computeIfAbsent(id, AbilitySource::new); }

    public static AbilitySource getAbilitySource(String namespace, String path) {
        return getAbilitySource(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static Optional<PlayerAbility> provideRegisteredAbility(Identifier id) {
        VanillaAbilities.init();
        return Optional.ofNullable(ABILITIES.get(id));
    }

    static Set<AbilitySource> sources(Player player, PlayerAbility ability) {
        return GRANTS.computeIfAbsent(player.level().isClientSide(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
            .computeIfAbsent(ability, k -> ConcurrentHashMap.newKeySet());
    }

    static boolean isGrantedByAnySource(Player player, PlayerAbility ability) { return !sources(player, ability).isEmpty(); }

    /** Platform hook: call on logout so stale grants don't leak. */
    public static void forget(Player player) {
        GRANTS.values().forEach(m -> m.remove(player.getUUID()));
    }

    /** Platform hook: call after a game-mode change / respawn to re-apply granted abilities. */
    public static void refreshAll(Player player) { ABILITIES.values().forEach(a -> a.refresh(player)); }
}
