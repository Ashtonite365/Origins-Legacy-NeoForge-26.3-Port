package io.github.apace100.apoli.integration;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

/**
 * NeoForge port: minimal stand-in for Fabric API's array-backed {@code Event}, keeping the same
 * {@code EVENT.register(...)} / {@code EVENT.invoker()} API for the Apoli/Origins callback interfaces.
 */
public final class Event<T> {
    private final Class<T> type;
    private final Function<T[], T> invokerFactory;
    private T[] handlers;
    private volatile T invoker;

    @SuppressWarnings("unchecked")
    Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        this.handlers = (T[]) Array.newInstance(type, 0);
        this.invoker = invokerFactory.apply(handlers);
    }

    public T invoker() {
        return invoker;
    }

    public synchronized void register(T listener) {
        if (listener == null) throw new NullPointerException("Tried to register a null " + type.getSimpleName() + " listener");
        handlers = Arrays.copyOf(handlers, handlers.length + 1);
        handlers[handlers.length - 1] = listener;
        invoker = invokerFactory.apply(handlers);
    }
}
