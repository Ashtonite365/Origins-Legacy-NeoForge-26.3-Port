package io.github.apace100.apoli.integration;


public interface PostPowerReloadCallback {

    Event<PostPowerReloadCallback> EVENT = EventFactory.createArrayBacked(PostPowerReloadCallback.class,
        (listeners) -> () -> {
            for (PostPowerReloadCallback event : listeners) {
                event.onPostPowerReload();
            }
        }
    );

    void onPostPowerReload();
}
