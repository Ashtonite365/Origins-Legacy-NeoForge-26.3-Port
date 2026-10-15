/*
 * Minimal local stand-in for PlayerAbilityLib (io.github.ladysnake.pal) used by the personal NeoForge port of
 * Origins: Legacy. Written from scratch against PAL's public API names only; no PAL (LGPL-3.0) code is copied.
 * Part of the Origins: Legacy port, MIT licensed (see LICENSE-apoli).
 */
package io.github.ladysnake.pal;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** A toggleable player ability backed by a vanilla {@link Abilities} flag. */
public final class PlayerAbility {
    private final Identifier id;
    private final BiConsumer<Abilities, Boolean> setter;
    private final Predicate<Abilities> getter;
    /** Whether the player's game mode grants this ability on its own (e.g. creative mayfly). */
    private final Predicate<Player> gameModeDefault;

    PlayerAbility(Identifier id, BiConsumer<Abilities, Boolean> setter, Predicate<Abilities> getter, Predicate<Player> gameModeDefault) {
        this.id = id; this.setter = setter; this.getter = getter; this.gameModeDefault = gameModeDefault;
    }

    public Identifier getId() { return id; }

    /** True if the vanilla flag is currently on for the player. */
    public boolean isEnabledFor(Player player) { return getter.test(player.getAbilities()); }

    void refresh(Player player) {
        boolean on = gameModeDefault.test(player) || Pal.isGrantedByAnySource(player, this);
        if (getter.test(player.getAbilities()) != on) {
            setter.accept(player.getAbilities(), on);
            if (!on && this == VanillaAbilities.ALLOW_FLYING) player.getAbilities().flying = false;
            if (player instanceof ServerPlayer sp) sp.onUpdateAbilities();
        }
    }

    @Override public String toString() { return "PlayerAbility[" + id + "]"; }
}
