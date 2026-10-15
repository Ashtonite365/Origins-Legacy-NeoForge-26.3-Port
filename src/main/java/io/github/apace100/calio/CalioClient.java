package io.github.apace100.calio;

import io.github.apace100.calio.network.CalioNetworkingClient;

/** NeoForge port: called from OriginsLegacyClientMod (client dist only). */
public class CalioClient {

    public void onInitializeClient() {
        CalioNetworkingClient.registerReceivers();
    }
}
