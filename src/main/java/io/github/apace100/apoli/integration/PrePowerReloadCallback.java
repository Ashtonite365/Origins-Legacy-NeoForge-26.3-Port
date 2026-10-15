package io.github.apace100.apoli.integration;


public interface PrePowerReloadCallback {

    Event<PrePowerReloadCallback> EVENT = EventFactory.createArrayBacked(PrePowerReloadCallback.class,
        (listeners) -> () -> {
            for (PrePowerReloadCallback event : listeners) {
                event.onPrePowerReload();
            }
        }
    );

    void onPrePowerReload();
}
