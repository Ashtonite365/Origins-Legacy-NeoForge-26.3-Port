/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.VanillaServerListeners;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Server data reload listeners (replaces ResourceManagerHelper.get(SERVER_DATA).registerReloadListener and
 * ResourceLoader.get(SERVER_DATA).registerReloadListener/addListenerOrdering).
 * Listeners are (re)added on every AddServerReloadListenersEvent; ordering uses the listener ids.
 */
public final class PlatformReloadListeners {
    private PlatformReloadListeners() {}

    /** Key of the last vanilla server listener (Fabric's ResourceReloaderKeys.AFTER_VANILLA equivalent: depend on this). */
    public static final Identifier AFTER_VANILLA = VanillaServerListeners.LAST;

    private record Reg(Identifier id, Function<HolderLookup.Provider, ? extends PreparableReloadListener> factory, Set<Identifier> after, Set<Identifier> before) {}
    private static final List<Reg> REGS = new ArrayList<>();
    private static final List<BiConsumer<AddServerReloadListenersEvent, HolderLookup.Provider>> RAW = new ArrayList<>();

    static void init() {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, PlatformReloadListeners::onAdd);
    }

    /** Register a Fabric-style identifiable listener; its getFabricDependencies() become "after" edges. Runs after vanilla. */
    public static void registerServer(IdentifiableResourceReloadListener listener) {
        Set<Identifier> after = new LinkedHashSet<>(listener.getFabricDependencies());
        after.add(AFTER_VANILLA);
        REGS.add(new Reg(listener.getFabricId(), p -> listener, after, new LinkedHashSet<>()));
    }

    /** Register a listener created per reload from the reload's registry lookup (replaces ResourceLoader.REGISTRY_LOOKUP_KEY use). */
    public static void registerServer(Identifier id, Function<HolderLookup.Provider, ? extends PreparableReloadListener> factory, Identifier... after) {
        Set<Identifier> a = new LinkedHashSet<>(Arrays.asList(after));
        a.add(AFTER_VANILLA);
        REGS.add(new Reg(id, factory, a, new LinkedHashSet<>()));
    }

    /** Fixed listener instance; {@code onReload} receives the registry lookup before the listener runs (e.g. powerTypes::setRegistryLookup). */
    public static <L extends PreparableReloadListener> void registerServer(Identifier id, L listener, BiConsumer<L, HolderLookup.Provider> onReload, Identifier... after) {
        registerServer(id, p -> { onReload.accept(listener, p); return listener; }, after);
    }

    /** Add an extra ordering edge: {@code first} must run before {@code second}. Unknown ids are ignored (logged). */
    public static void addOrdering(Identifier first, Identifier second) {
        for (Reg r : REGS) {
            if (r.id().equals(second)) r.after().add(first);
            if (r.id().equals(first)) r.before().add(second);
        }
    }

    /** Escape hatch: direct access to the event on each reload. */
    public static void onServerReload(BiConsumer<AddServerReloadListenersEvent, HolderLookup.Provider> hook) {
        RAW.add(hook);
    }

    private static void onAdd(AddServerReloadListenersEvent event) {
        HolderLookup.Provider lookup = event.getServerResources().getRegistryLookup();
        for (Reg r : REGS) event.addListener(r.id(), r.factory().apply(lookup));
        for (Reg r : REGS) {
            for (Identifier a : r.after()) edge(event, a, r.id());
            for (Identifier b : r.before()) edge(event, r.id(), b);
        }
        RAW.forEach(h -> h.accept(event, lookup));
    }

    private static void edge(AddServerReloadListenersEvent event, Identifier first, Identifier second) {
        if (event.getRegistry().containsKey(first) && event.getRegistry().containsKey(second)) {
            event.addDependency(first, second);
        } else {
            org.apache.logging.log4j.LogManager.getLogger("origins-platform").warn("Skipping reload ordering {} -> {} (unknown listener)", first, second);
        }
    }
}
