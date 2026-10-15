/*
 * Origins: Legacy personal NeoForge port. Calio-Legacy is MIT, (c) 2021 apace100 (see LICENSE-calio).
 */
package io.github.apace100.calio.resource;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.SortedReloadListenerEvent;
import xyz.bluspring.originslegacy.platform.IdentifiableResourceReloadListener;
import xyz.bluspring.originslegacy.platform.PlatformReloadListeners;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * NeoForge replacement for Calio's Fabric ResourceManagerHelper registration and its ReloadableServerResources mixin.
 * Calio queues listeners here (in the dependency order computed by OrderedResourceListenerManager / OrderedResourceListeners);
 * the mod's reload-listener wiring (H6) calls the two static helpers from the NeoForge events.
 */
public final class CalioReloadListeners {
    private CalioReloadListeners() {}

    /** A listener created per reload from the reload's registry lookup (fixed listeners ignore the lookup). */
    public record Entry(Identifier id, Function<HolderLookup.Provider, ? extends PreparableReloadListener> factory) {}

    private static final List<Entry> SERVER = new ArrayList<>();
    private static final List<IdentifiableResourceReloadListener> CLIENT = new ArrayList<>();

    /** Queue a fixed server listener (used by Calio's util.OrderedResourceListeners). */
    public static synchronized void queueServer(IdentifiableResourceReloadListener listener) {
        SERVER.add(new Entry(listener.getFabricId(), p -> listener));
    }

    static synchronized void queueRegistryBased(Identifier id, Function<HolderLookup.Provider, ? extends PreparableReloadListener> factory) {
        Entry e = new Entry(id, factory);
        SERVER.add(e);
        OrderedResourceListenerManager.getRegistryBasedReloadListenerProviders().add(e);
    }

    static synchronized void queueClient(IdentifiableResourceReloadListener listener) {
        CLIENT.add(listener);
    }

    /** H6: call from an {@code AddServerReloadListenersEvent} listener (game bus). Adds all Calio server listeners after vanilla, in order. */
    public static synchronized void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        OrderedResourceListener.finishIfNeeded();
        HolderLookup.Provider lookup = event.getServerResources().getRegistryLookup();
        Identifier prev = PlatformReloadListeners.AFTER_VANILLA;
        for (Entry e : SERVER) prev = add(event, e.id(), e.factory().apply(lookup), prev);
    }

    /** H6: call from an {@code AddClientReloadListenersEvent} listener (mod bus, client only). */
    public static synchronized void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        OrderedResourceListener.finishIfNeeded();
        Identifier prev = net.neoforged.neoforge.client.resources.VanillaClientListeners.LAST;
        for (IdentifiableResourceReloadListener l : CLIENT) prev = add(event, l.getFabricId(), l, prev);
    }

    private static Identifier add(SortedReloadListenerEvent event, Identifier id, PreparableReloadListener listener, Identifier after) {
        event.addListener(id, listener);
        if (event.getRegistry().containsKey(after)) event.addDependency(after, id);
        return id;
    }
}
