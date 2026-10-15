/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/** Loader queries (replaces FabricLoader) and access to the mod event bus. */
public final class Platform {
    /** FML mod id of this publish build. Content namespaces stay "origins"/"apoli"/"calio" (see Origins.MODID). */
    public static final String MOD_ID = "origins_legacy_port263";
    private static IEventBus modBus;
    private static ModContainer container;

    private Platform() {}

    /** Called once from {@link xyz.bluspring.originslegacy.OriginsLegacyMod}. */
    public static void init(IEventBus bus, ModContainer modContainer) {
        if (modBus != null) return;
        modBus = bus;
        container = modContainer;
        PlatformRegistries.init(bus);
        PlatformNetworking.init(bus);
        PlatformReloadListeners.init();
        PalHooks.init();
        xyz.bluspring.originslegacy.platform.component.PlatformComponents.init(bus);
    }

    public static IEventBus modBus() {
        if (modBus == null) throw new IllegalStateException("Platform not initialised yet");
        return modBus;
    }

    /** The game (NeoForge) event bus, i.e. NeoForge.EVENT_BUS. */
    public static IEventBus gameBus() { return NeoForge.EVENT_BUS; }

    public static ModContainer container() { return container; }

    public static boolean isModLoaded(String modId) {
        ModList list = ModList.get();
        return list != null && list.isLoaded(modId);
    }

    /** Mod version string (e.g. for Apoli.VERSION). Empty if the mod is absent. */
    public static Optional<String> getModVersion(String modId) {
        ModList list = ModList.get();
        if (list == null) return Optional.empty();
        return list.getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString());
    }

    public static boolean isClient() { return FMLEnvironment.getDist().isClient(); }
    public static boolean isDedicatedServer() { return FMLEnvironment.getDist().isDedicatedServer(); }
    public static boolean isDevelopmentEnvironment() { return !FMLEnvironment.isProduction(); }
    public static Path getConfigDir() { return FMLPaths.CONFIGDIR.get(); }
    public static Path getGameDir() { return FMLPaths.GAMEDIR.get(); }

    /** The running logical server, or null. */
    public static @Nullable MinecraftServer currentServer() { return ServerLifecycleHooks.getCurrentServer(); }
}
