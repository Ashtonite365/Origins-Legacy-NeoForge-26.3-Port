package io.github.apace100.origins;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.EventPriority;
import xyz.bluspring.originslegacy.platform.Platform;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;
import xyz.bluspring.originslegacy.platform.PlatformReloadListeners;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.util.NamespaceAlias;
import io.github.apace100.calio.resource.CalioReloadListeners;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.command.OriginCommand;
import io.github.apace100.origins.component.OriginTargetsComponent;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.networking.ModPacketsC2S;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginManager;
import io.github.apace100.origins.power.OriginsEntityConditions;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.*;
import io.github.apace100.origins.util.ChoseOriginCriterion;
import io.github.apace100.origins.util.OriginsConfigSerializer;
import io.github.apace100.origins.util.OriginsJsonConfigSerializer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTabs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * NeoForge port: plain init class called by H1's shim. {@link #init} replaces the Fabric ModInitializer and
 * OrderedResourceListenerInitializer entrypoints of Origins and (via {@link Apoli#init}) Apoli.
 */
public class Origins {

	public static final String MODID = "origins";
	public static final String LEGACY_MODID = "origins_legacy";

	public static String VERSION = "";
	public static int[] SEMVER;
	public static final Logger LOGGER = LogManager.getLogger(Origins.class);

	public static ServerConfig config;
	private static ConfigSerializer<ServerConfig> configSerializer;

	/** Called once by H1's shim (xyz.bluspring.originslegacy.OriginsLegacyMod). Runs Apoli.init first. */
	public static void init(IEventBus modBus, ModContainer container) {
		Apoli.init(modBus, container);

		if (!FMLEnvironment.isProduction() && System.getProperty("origins.audit", "false").equalsIgnoreCase("true")) {
			MixinEnvironment.getCurrentEnvironment().audit();
		}

		ModPackets.init();
		{
			VERSION = container.getModInfo().getVersion().toString();
			if(VERSION.contains("+")) {
				VERSION = VERSION.split("\\+")[0];
			}
			if(VERSION.contains("-")) {
				VERSION = VERSION.split("-")[0];
			}
			String[] splitVersion = VERSION.split("\\.");
			SEMVER = new int[splitVersion.length];
			for(int i = 0; i < SEMVER.length; i++) {
				SEMVER[i] = Integer.parseInt(splitVersion[i]);
			}
		}
		LOGGER.info("Origins " + VERSION + " is initializing. Have fun!");

		AutoConfig.register(ServerConfig.class,
			(definition, configClass) -> {
				configSerializer = new OriginsJsonConfigSerializer<>(definition, configClass,
					new OriginsConfigSerializer<>(definition, configClass));
				return configSerializer;
			});
		config = AutoConfig.getConfigHolder(ServerConfig.class).getConfig();

		NamespaceAlias.addAlias(MODID, Apoli.MODID);
		NamespaceAlias.addAlias(LEGACY_MODID, Apoli.LEGACY_MODID);

		OriginsPowerTypes.register();
		OriginsEntityConditions.register();

		ModBlocks.register(modBus);
		ModItems.register(modBus);
		ModTags.register();
		ModPacketsC2S.register();
		ModEnchantments.register();
		ModEntities.register(modBus);
		ModLoot.registerLootTables();
		Origin.init();

		Platform.gameBus().addListener((RegisterCommandsEvent event) -> OriginCommand.register(event.getDispatcher()));
		modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
				event.accept(ModItems.ORB_OF_ORIGIN.get());
			}
		});
		PlatformRegistries.register(Registries.TRIGGER_TYPE, Origins.identifier("chose_origin"), ChoseOriginCriterion.INSTANCE);
		PlatformRegistries.register(Registries.DATA_COMPONENT_TYPE, Origins.identifier("origin_targets"), OriginTargetsComponent.TYPE);
		// O-L: my bad. (old "choose_origin" id kept as an alias of "chose_origin"); added once the entry is registered.
		modBus.addListener(EventPriority.LOWEST, (RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.TRIGGER_TYPE)) {
				BuiltInRegistries.TRIGGER_TYPES.addAlias(Origins.identifier("choose_origin"), Origins.identifier("chose_origin"));
			}
		});
		registerResourceListeners();
		// Calio-ordered server listeners (H3): wired exactly once here, not through PlatformReloadListeners.
		Platform.gameBus().addListener(CalioReloadListeners::onAddServerReloadListeners);
	}

	public static void serializeConfig() {
		try {
			configSerializer.serialize(config);
		} catch (ConfigSerializer.SerializationException e) {
			Origins.LOGGER.error("Failed serialization of config file: " + e.getMessage());
		}
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(Origins.MODID, path);
	}

	public static Identifier legacy(String path) {
		return Identifier.fromNamespaceAndPath(LEGACY_MODID, path);
	}

	private static void registerResourceListeners() {
		Identifier powerData = Apoli.identifier("powers");
		Identifier originData = Origins.identifier("origins");
		Identifier layerData = Origins.identifier("origin_layers");
		Identifier badgeData = Origins.identifier("badge"); // DataObjectRegistry loader id (was getFabricId())

		// Order: powers -> origins -> origin_layers; powers -> badge.
		PlatformReloadListeners.registerServer(originData, lookup -> new OriginManager(), powerData);
		PlatformReloadListeners.registerServer(layerData, lookup -> new OriginLayers(), originData);

		BadgeManager.init();
		// Upstream orders badges both after and before powers (a no-op pair under Fabric's sorter, a cycle under NeoForge's);
		// badges need power types, so they run after powers.
		PreparableReloadListener badgeLoader = BadgeManager.REGISTRY.getLoader();
		PlatformReloadListeners.registerServer(badgeData, lookup -> badgeLoader, powerData);
	}

	@Config(name = Origins.MODID + "_server")
	public static class ServerConfig implements ConfigData {

		public boolean performVersionCheck = true;

		public JsonObject origins = new JsonObject();

		public boolean isOriginDisabled(Identifier originId) {
			String idString = originId.toString();
			if(!origins.has(idString)) {
				return false;
			}
			JsonElement element = origins.get(idString);
			if(element instanceof JsonObject jsonObject) {
				return !GsonHelper.getAsBoolean(jsonObject, "enabled", true);
			}
			return false;
		}

		public boolean isPowerDisabled(Identifier originId, Identifier powerId) {
			String originIdString = originId.toString();
			if(!origins.has(originIdString)) {
				return false;
			}
			String powerIdString = powerId.toString();
			JsonElement element = origins.get(originIdString);
			if(element instanceof JsonObject jsonObject) {
				return !GsonHelper.getAsBoolean(jsonObject, powerIdString, true);
			}
			return false;
		}

		public boolean addToConfig(Origin origin) {
			boolean changed = false;
			String originIdString = origin.getIdentifier().toString();
			JsonObject originObj;
			if(!origins.has(originIdString) || !(origins.get(originIdString) instanceof JsonObject)) {
				originObj = new JsonObject();
				origins.add(originIdString, originObj);
				changed = true;
			} else {
				originObj = (JsonObject) origins.get(originIdString);
			}
			if(!originObj.has("enabled") || !(originObj.get("enabled") instanceof JsonPrimitive)) {
				originObj.addProperty("enabled", Boolean.TRUE);
				changed = true;
			}
			for(PowerType<?> power : origin.getPowerTypes()) {
				String powerIdString = power.getIdentifier().toString();
				if(!originObj.has(powerIdString) || !(originObj.get(powerIdString) instanceof JsonPrimitive)) {
					originObj.addProperty(powerIdString, Boolean.TRUE);
					changed = true;
				}
			}
			return changed;
		}
	}
}
