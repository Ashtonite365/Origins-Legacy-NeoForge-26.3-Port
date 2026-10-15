package io.github.apace100.apoli.integration;

import java.util.function.Function;

/** NeoForge port: replacement for Fabric API's {@code EventFactory.createArrayBacked}. */
public final class EventFactory {
    private EventFactory() {
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> invokerFactory) {
        return new Event<>(type, invokerFactory);
    }
}
