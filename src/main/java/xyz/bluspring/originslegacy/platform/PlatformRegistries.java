/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registration helpers (replace {@code Registry.register(BuiltInRegistries.X, id, v)} and FabricRegistryBuilder).
 * NeoForge freezes vanilla registries outside RegisterEvent, so values are queued and registered there.
 */
public final class PlatformRegistries {
    private PlatformRegistries() {}

    private record Entry<T>(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<? extends T> value) {}
    private static final List<Entry<?>> ENTRIES = new ArrayList<>();
    private static final List<Registry<?>> NEW_REGISTRIES = new ArrayList<>();

    static void init(IEventBus bus) {
        bus.addListener(RegisterEvent.class, PlatformRegistries::onRegister);
        bus.addListener(NewRegistryEvent.class, e -> NEW_REGISTRIES.forEach(e::register));
    }

    /** Queue an already constructed value; returns it unchanged (use for objects referenced directly, e.g. DataComponentType, serializers). */
    public static <T, V extends T> V register(ResourceKey<? extends Registry<T>> registry, Identifier id, V value) {
        add(new Entry<T>(registry, id, () -> value));
        return value;
    }

    /** Queue a lazily constructed value and get a holder (use where a {@code Holder<T>} is needed, e.g. attributes, effects). */
    public static <T, V extends T> DeferredHolder<T, V> registerHolder(ResourceKey<? extends Registry<T>> registry, Identifier id, Supplier<V> value) {
        add(new Entry<T>(registry, id, value));
        return DeferredHolder.create(registry, id);
    }

    private static void add(Entry<?> e) {
        ENTRIES.add(e);
    }

    /**
     * Create a custom (simple, unsynced) registry, replacing FabricRegistryBuilder.createSimple(key).buildAndRegister().
     * Must be called during mod construction.
     */
    public static <T> Registry<T> createSimple(ResourceKey<Registry<T>> key) {
        Registry<T> reg = new RegistryBuilder<>(key).create();
        NEW_REGISTRIES.add(reg);
        return reg;
    }

    /** As {@link #createSimple} but synced to clients. */
    public static <T> Registry<T> createSynced(ResourceKey<Registry<T>> key) {
        Registry<T> reg = new RegistryBuilder<>(key).sync(true).create();
        NEW_REGISTRIES.add(reg);
        return reg;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onRegister(RegisterEvent event) {
        for (Entry e : ENTRIES) {
            if (event.getRegistryKey().equals(e.registry())) {
                event.register((ResourceKey) e.registry(), e.id(), e.value());
            }
        }
    }

}
