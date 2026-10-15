/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform.event;

import java.util.function.Function;

/** Replacement for Fabric's EventFactory (same method name). */
public final class EventFactory {
    private EventFactory() {}

    public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
        return new Event<>(type, invokerFactory);
    }

    /** Variant with an explicit empty invoker (Fabric overload); the empty invoker is ignored here. */
    public static <T> Event<T> createArrayBacked(Class<? super T> type, T emptyInvoker, Function<T[], T> invokerFactory) {
        return new Event<>(type, invokerFactory);
    }
}
