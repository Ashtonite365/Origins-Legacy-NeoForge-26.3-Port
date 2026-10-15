package io.github.apace100.apoli.integration;


@Deprecated
public interface PowerReloadCallback {

    Event<PowerReloadCallback> EVENT = EventFactory.createArrayBacked(PowerReloadCallback.class,
        (listeners) -> () -> {
            for (PowerReloadCallback event : listeners) {
                event.onPowerReload();
            }
        }
    );

    void onPowerReload();
}
