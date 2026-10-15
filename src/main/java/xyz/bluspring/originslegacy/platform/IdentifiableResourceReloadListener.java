/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Collection;
import java.util.List;

/** Same shape as Fabric's IdentifiableResourceReloadListener; swap the import. Register with {@link PlatformReloadListeners}. */
public interface IdentifiableResourceReloadListener extends PreparableReloadListener {
    Identifier getFabricId();

    /** Ids of listeners that must run before this one. */
    default Collection<Identifier> getFabricDependencies() { return List.of(); }
}
