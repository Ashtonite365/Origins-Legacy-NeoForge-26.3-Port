/*
 * Minimal local stand-in for PlayerAbilityLib (io.github.ladysnake.pal) used by the personal NeoForge port of
 * Origins: Legacy. Written from scratch against PAL's public API names only; no PAL (LGPL-3.0) code is copied.
 * Part of the Origins: Legacy port, MIT licensed (see LICENSE-apoli).
 */
package io.github.ladysnake.pal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** A named reason for granting abilities (e.g. one power). Grants are tracked per player in {@link Pal}. */
public final class AbilitySource {
    private final Identifier id;
    AbilitySource(Identifier id) { this.id = id; }
    public Identifier getId() { return id; }

    public boolean grants(Player player, PlayerAbility ability) { return Pal.sources(player, ability).contains(this); }

    public void grantTo(Player player, PlayerAbility ability) {
        if (Pal.sources(player, ability).add(this)) ability.refresh(player);
    }

    public void revokeFrom(Player player, PlayerAbility ability) {
        if (Pal.sources(player, ability).remove(this)) ability.refresh(player);
    }
}
