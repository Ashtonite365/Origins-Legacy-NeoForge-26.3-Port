/*
 * Origins: Legacy personal NeoForge port. MIT, (c) 2020-2021 apace100; forward port by BluSpring (see LICENSE*).
 */
package xyz.bluspring.originslegacy;

import io.github.apace100.apoli.ApoliClient;
import io.github.apace100.calio.CalioClient;
import io.github.apace100.origins.OriginsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import xyz.bluspring.originslegacy.platform.Platform;

/** Client entrypoints (fabric.mod.json "client"), run after {@link OriginsLegacyMod}. H6 contract: {@code OriginsClient.init(IEventBus, ModContainer)}, called once (also registers the config screen). */
@Mod(value = Platform.MOD_ID, dist = Dist.CLIENT)
public final class OriginsLegacyClientMod {
    public OriginsLegacyClientMod(IEventBus modBus, ModContainer container) {
        new CalioClient().onInitializeClient();
        // ApoliClient (H1) registers Apoli's S2C receivers + active-power key tick; idempotent.
        new ApoliClient().onInitializeClient();
        OriginsClient.init(modBus, container);
    }
}
