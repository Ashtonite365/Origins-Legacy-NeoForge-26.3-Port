package io.github.apace100.apoli;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import xyz.bluspring.originslegacy.platform.Platform;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;
import xyz.bluspring.originslegacy.platform.PlatformReloadListeners;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import de.dafuqs.additionalentityattributes.AdditionalEntityAttributes;
import io.github.apace100.apoli.access.IdentifiedLootTable;
import io.github.apace100.apoli.command.PowerCommand;
import io.github.apace100.apoli.command.ResourceCommand;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.component.PowerHolderComponentImpl;
import io.github.apace100.apoli.component.StackPowerComponent;
import io.github.apace100.apoli.global.GlobalPowerSetLoader;
import io.github.apace100.apoli.util.ApoliResourceConditions;
import io.github.apace100.apoli.networking.ModPackets;
import io.github.apace100.apoli.networking.ModPacketsC2S;
import io.github.apace100.apoli.power.PowerTypes;
import io.github.apace100.apoli.power.factory.PowerFactories;
import io.github.apace100.apoli.power.factory.action.BiEntityActions;
import io.github.apace100.apoli.power.factory.action.BlockActions;
import io.github.apace100.apoli.power.factory.action.EntityActions;
import io.github.apace100.apoli.power.factory.action.ItemActions;
import io.github.apace100.apoli.power.factory.condition.*;
import io.github.apace100.apoli.registry.ApoliClassData;
import io.github.apace100.apoli.util.*;
import io.github.apace100.apoli.util.modifier.ModifierOperations;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * NeoForge port: Apoli is not a separate mod any more. {@link #init} is called first from the {@code origins}
 * shim via {@link io.github.apace100.origins.Origins#init} and replaces the Fabric ModInitializer,
 * EntityComponentInitializer (CCA -> attachments, owned by H1) and resource-listener entrypoints.
 */
public class Apoli {

	public static ApoliConfig config;

	public static MinecraftServer server;

	public static final Scheduler SCHEDULER = Scheduler.get(); // shared singleton (H4)

	public static final String MODID = "apoli";
	public static final String LEGACY_MODID = "apoli_legacy";

	public static final Logger LOGGER = LogManager.getLogger(Apoli.class);
	public static String VERSION = "";
	public static int[] SEMVER;

	public static final AbilitySource LEGACY_POWER_SOURCE = Pal.getAbilitySource(Apoli.identifier("power_source"));

	public static final boolean PERFORM_VERSION_CHECK = false;

	public static void init(IEventBus modBus, ModContainer container) {
		AdditionalEntityAttributes.register(modBus); // H4-owned class
		ModPackets.init();
		Platform.gameBus().addListener((ServerStartedEvent event) -> server = event.getServer());

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

		ModPacketsC2S.register();

		Platform.gameBus().addListener((RegisterCommandsEvent event) -> {
			PowerCommand.register(event.getDispatcher());
			ResourceCommand.register(event.getDispatcher());
		});

		// Vanilla registries are frozen on NeoForge: register through H1's PlatformRegistries (RegisterEvent-backed).
		PlatformRegistries.register(Registries.RECIPE_SERIALIZER, Apoli.identifier("power_restricted"), PowerRestrictedCraftingRecipe.SERIALIZER);
		PlatformRegistries.register(Registries.RECIPE_SERIALIZER, Apoli.identifier("modified"), ModifiedCraftingRecipe.SERIALIZER);
		PlatformRegistries.register(Registries.DATA_COMPONENT_TYPE, Apoli.identifier("stack_powers"), StackPowerComponent.TYPE);
		PlatformRegistries.register(Registries.LOOT_FUNCTION_TYPE, Apoli.identifier("add_power"), AddPowerLootFunction.CODEC);
		PlatformRegistries.register(Registries.LOOT_FUNCTION_TYPE, Apoli.identifier("remove_power"), RemovePowerLootFunction.CODEC);
		PlatformRegistries.register(Registries.LOOT_CONDITION_TYPE, Apoli.identifier("power"), PowerLootCondition.CODEC);
		PlatformRegistries.register(Registries.TRIGGER_TYPE, Apoli.identifier("gained_power"), GainedPowerCriterion.INSTANCE);

		ApoliClassData.registerAll();

		ModifierOperations.registerAll();

		PowerFactories.register();
		EntityConditions.register();
		BiEntityConditions.register();
		ItemConditions.register();
		BlockConditions.register();
		DamageConditions.register();
		FluidConditions.register();
		BiomeConditions.register();
		EntityActions.register();
		ItemActions.register();
		BlockActions.register();
		BiEntityActions.register();

		ApoliResourceConditions.register(modBus); // H4 (ol-h4 33e0b5c): NeoForge condition codecs, same ids

		LOGGER.info("Apoli " + VERSION + " has initialized. Ready to power up your game!");

		// Legacy: Updating the world can sometimes encounter chunk bans caused by Additional Entity Attributes. Which is weird, considering it should be getting datafixed automatically.
		var genericAttributes = List.of(
			AdditionalEntityAttributes.CRITICAL_BONUS_DAMAGE,
			AdditionalEntityAttributes.WATER_SPEED,
			AdditionalEntityAttributes.LAVA_SPEED,
			AdditionalEntityAttributes.WIDTH,
			AdditionalEntityAttributes.HEIGHT,
			AdditionalEntityAttributes.HITBOX_SCALE,
			AdditionalEntityAttributes.HITBOX_WIDTH,
			AdditionalEntityAttributes.HITBOX_HEIGHT,
			AdditionalEntityAttributes.MODEL_SCALE,
			AdditionalEntityAttributes.MODEL_HEIGHT,
			AdditionalEntityAttributes.MODEL_WIDTH,
			AdditionalEntityAttributes.MOB_DETECTION_RANGE,
			AdditionalEntityAttributes.MAGIC_PROTECTION
		);

		var playerAttributes = List.of(
			AdditionalEntityAttributes.WATER_VISIBILITY,
			AdditionalEntityAttributes.LAVA_VISIBILITY,
			AdditionalEntityAttributes.BONUS_LOOT_COUNT_ROLLS,
			AdditionalEntityAttributes.BONUS_RARE_LOOT_ROLLS,
			AdditionalEntityAttributes.DROPPED_EXPERIENCE,
			AdditionalEntityAttributes.COLLECTION_RANGE
		);

		// Aliases must be added once the attributes are registered (holders bound).
		modBus.addListener(EventPriority.LOWEST, (RegisterEvent event) -> {
			if (!event.getRegistryKey().equals(Registries.ATTRIBUTE)) return;
			for (Holder<Attribute> attribute : genericAttributes) {
				var id = attribute.unwrapKey().orElseThrow().identifier();
				BuiltInRegistries.ATTRIBUTE.addAlias(id.withPrefix("generic."), id);
			}

			for (Holder<Attribute> attribute : playerAttributes) {
				var id = attribute.unwrapKey().orElseThrow().identifier();
				BuiltInRegistries.ATTRIBUTE.addAlias(id.withPrefix("player."), id);
			}
		});

		registerResourceListeners();

		Platform.gameBus().addListener((ServerStartedEvent event) -> bindLootTableKeys(event.getServer()));
		// END_DATA_PACK_RELOAD -> OnDatapackSyncEvent with no player (fires once after a successful /reload).
		Platform.gameBus().addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null) {
				bindLootTableKeys(event.getPlayerList().getServer());
			}
		});
	}

	private static void registerResourceListeners() {
		// PlatformReloadListeners: every listener runs after vanilla. Order: powers -> global_powers.
		PlatformReloadListeners.registerServer(identifier("powers"), lookup -> new PowerTypes()); // lookup injected during reload (ContextAwareReloadListener)
		PlatformReloadListeners.registerServer(identifier("global_powers"), lookup -> new GlobalPowerSetLoader(), identifier("powers"));
	}

	private static void bindLootTableKeys(MinecraftServer server) {
		server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
			.listElements().forEach(holder -> {
				((IdentifiedLootTable) holder.value()).apoli$setId(holder.key().identifier(), server.registryAccess());
			});
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}

	public static Identifier legacy(String path) {
		return Identifier.fromNamespaceAndPath(LEGACY_MODID, path);
	}

	// CCA registerEntityComponentFactories / OrderedResourceListenerInitializer removed: PowerHolderComponent is a
	// NeoForge data attachment now (apoli/component, H1), copied on death like RespawnCopyStrategy.ALWAYS_COPY.
}
