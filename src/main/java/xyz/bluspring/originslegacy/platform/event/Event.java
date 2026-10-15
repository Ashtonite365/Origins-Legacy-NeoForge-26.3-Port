/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform.event;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Drop-in replacement for Fabric's {@code net.fabricmc.fabric.api.event.Event}: callbacks are added with
 * {@link #register(Object)} and fired through {@link #invoker()}. Create with {@link EventFactory#createArrayBacked}.
 */
public final class Event<T> {
    private final Class<? super T> type;
    private final Function<T[], T> invokerFactory;
    private final CopyOnWriteArrayList<T> listeners = new CopyOnWriteArrayList<>();
    private volatile T invoker;

    Event(Class<? super T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        rebuild();
    }

    public T invoker() { return invoker; }

    public void register(T listener) {
        listeners.add(listener);
        rebuild();
    }

    @SuppressWarnings("unchecked")
    private void rebuild() {
        T[] arr = (T[]) java.lang.reflect.Array.newInstance(type, listeners.size());
        invoker = invokerFactory.apply(listeners.toArray(arr));
    }
}
