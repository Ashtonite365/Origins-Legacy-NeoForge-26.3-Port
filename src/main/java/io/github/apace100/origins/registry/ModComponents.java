package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.component.PlayerOriginComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import xyz.bluspring.originslegacy.platform.component.ComponentKey;
import xyz.bluspring.originslegacy.platform.component.RespawnCopyStrategy;

/** NeoForge port: CCA entity component -> data attachment on players, copied on respawn (CCA CHARACTER). */
public class ModComponents {

    public static final ComponentKey<OriginComponent> ORIGIN = ComponentKey.create(
        Identifier.fromNamespaceAndPath(Origins.MODID, "origin"), OriginComponent.class,
        Player.class, PlayerOriginComponent::new, RespawnCopyStrategy.CHARACTER);
}
