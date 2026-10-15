package io.github.apace100.apoli.integration;

import io.github.apace100.apoli.util.ApoliConfigClient;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge port: ModMenu is Fabric-only. The config screen is exposed through NeoForge's mod list instead;
 * call {@link #register(ModContainer)} from the client mod constructor.
 */
public class ModMenuIntegration {

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (modContainer, parent) -> AutoConfigClient.getConfigScreen(ApoliConfigClient.class, parent).get());
    }
}
