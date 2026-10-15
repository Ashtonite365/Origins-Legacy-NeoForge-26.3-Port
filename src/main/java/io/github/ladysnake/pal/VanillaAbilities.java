/*
 * Minimal local stand-in for PlayerAbilityLib (io.github.ladysnake.pal) used by the personal NeoForge port of
 * Origins: Legacy. Written from scratch against PAL's public API names only; no PAL (LGPL-3.0) code is copied.
 * Part of the Origins: Legacy port, MIT licensed (see LICENSE-apoli).
 */
package io.github.ladysnake.pal;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class VanillaAbilities {
    private VanillaAbilities() {}

    private static boolean creativeLike(Player p) { return p.isCreative() || p.isSpectator(); }

    public static final PlayerAbility ALLOW_FLYING = Pal.registerAbility(new PlayerAbility(
        Identifier.withDefaultNamespace("allow_flying"), (a, v) -> a.mayfly = v, a -> a.mayfly, VanillaAbilities::creativeLike));
    public static final PlayerAbility FLYING = Pal.registerAbility(new PlayerAbility(
        Identifier.withDefaultNamespace("flying"), (a, v) -> a.flying = v, a -> a.flying, Player::isSpectator));
    public static final PlayerAbility INVULNERABLE = Pal.registerAbility(new PlayerAbility(
        Identifier.withDefaultNamespace("invulnerable"), (a, v) -> a.invulnerable = v, a -> a.invulnerable, VanillaAbilities::creativeLike));
    public static final PlayerAbility LIMIT_WORLD_MODIFICATIONS = Pal.registerAbility(new PlayerAbility(
        Identifier.withDefaultNamespace("limit_world_modifications"), (a, v) -> a.mayBuild = !v, a -> !a.mayBuild, p -> false));

    static void init() {}
}
