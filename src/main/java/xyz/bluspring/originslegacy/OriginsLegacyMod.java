/*
 * Origins: Legacy personal NeoForge port. MIT, (c) 2020-2021 apace100; forward port by BluSpring (see LICENSE*).
 */
package xyz.bluspring.originslegacy;

import io.github.apace100.apoli.ApoliServer;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.resource.OrderedResourceListener;
import io.github.apace100.origins.registry.ModComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import xyz.bluspring.originslegacy.platform.Platform;

/**
 * Single NeoForge entrypoint for the three bundled Fabric mods (calio, apoli, origins), replacing the
 * fabric.mod.json "main"/"server" entrypoints in their original order. Client entrypoints: {@link OriginsLegacyClientMod}.
 * Contract: Calio/OrderedResourceListener keep {@code onInitialize()}, ApoliServer {@code onInitializeServer()};
 * {@code Origins.init(IEventBus, ModContainer)} (H6) does Origins + Apoli. No other class may carry {@code @Mod}.
 */
@Mod(Platform.MOD_ID)
public final class OriginsLegacyMod {
    public OriginsLegacyMod(IEventBus modBus, ModContainer container) {
        Platform.init(modBus, container);
        // Component keys must exist before RegisterEvent (attachment types).
        bootstrapComponents();
        io.github.apace100.apoli.registry.ApoliArgumentTypes.register();
        io.github.apace100.origins.registry.ModArgumentTypes.register();

        new Calio().onInitialize();
        new OrderedResourceListener().onInitialize();
        // H6 contract: Origins.init calls Apoli.init itself. Called exactly once.
        io.github.apace100.origins.Origins.init(modBus, container);
        if (Platform.isDedicatedServer()) {
            new ApoliServer().onInitializeServer();
        }
    }

    private static void bootstrapComponents() {
        java.util.Objects.requireNonNull(PowerHolderComponent.KEY);
        java.util.Objects.requireNonNull(ModComponents.ORIGIN);
    }
}
