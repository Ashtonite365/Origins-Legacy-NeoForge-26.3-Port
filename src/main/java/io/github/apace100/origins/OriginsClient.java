package io.github.apace100.origins;

import io.github.apace100.calio.resource.CalioReloadListeners;
import io.github.apace100.origins.integration.ModMenuIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import xyz.bluspring.originslegacy.platform.Platform;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.apace100.apoli.ApoliClient;
import io.github.apace100.apoli.integration.PowerClearCallback;
import io.github.apace100.origins.networking.ModPacketsS2C;
import io.github.apace100.origins.registry.ModEntities;
import io.github.apace100.origins.screen.ViewOriginScreen;
import io.github.apace100.origins.util.PowerKeyManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class OriginsClient {
    public static final KeyMapping.Category ORIGINS_CATEGORY = new KeyMapping.Category(Origins.identifier("origins")); // registered via RegisterKeyMappingsEvent;

    public static KeyMapping usePrimaryActivePowerKeybind;
    public static KeyMapping useSecondaryActivePowerKeybind;
    public static KeyMapping viewCurrentOriginKeybind;

    public static boolean isServerRunningOrigins = false;

    /** Client-only; called once by H1's shim (OriginsLegacyClientMod). */
    public static void init(IEventBus modBus, ModContainer container) {
        ModMenuIntegration.register(container);
        // Calio-ordered client listeners (H3): wired exactly once, client-only, on the mod bus.
        modBus.addListener(CalioReloadListeners::onAddClientReloadListeners);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(ModEntities.ENDERIAN_PEARL.get(), ThrownItemRenderer::new));

        ModPacketsS2C.register();

        usePrimaryActivePowerKeybind = new KeyMapping("key.origins.primary_active", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, ORIGINS_CATEGORY);
        useSecondaryActivePowerKeybind = new KeyMapping("key.origins.secondary_active", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), ORIGINS_CATEGORY);
        viewCurrentOriginKeybind = new KeyMapping("key.origins.view_origin", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, ORIGINS_CATEGORY);

        ApoliClient.registerPowerKeybinding("key.layers.primary_active", usePrimaryActivePowerKeybind);
        ApoliClient.registerPowerKeybinding("key.layers.secondary_active", useSecondaryActivePowerKeybind);
        ApoliClient.registerPowerKeybinding("primary", usePrimaryActivePowerKeybind);
        ApoliClient.registerPowerKeybinding("secondary", useSecondaryActivePowerKeybind);

        // "none" is the default key used when none is specified.
        ApoliClient.registerPowerKeybinding("none", usePrimaryActivePowerKeybind);

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(ORIGINS_CATEGORY);
            event.register(usePrimaryActivePowerKeybind);
            event.register(useSecondaryActivePowerKeybind);
            event.register(viewCurrentOriginKeybind);
        });

        Platform.gameBus().addListener((ClientTickEvent.Pre tick) -> {
            while(viewCurrentOriginKeybind.consumeClick()) {
                if(!(Minecraft.getInstance().gui.screen() instanceof ViewOriginScreen)) {
                    Minecraft.getInstance().gui.setScreen(new ViewOriginScreen());
                }
            }
        });

        PowerClearCallback.EVENT.register(PowerKeyManager::clearCache);
    }
}
