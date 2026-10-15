package io.github.apace100.apoli.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.PowerTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApoliResourceConditions {

	/** Fabric's ResourceConditions.CONDITIONS_KEY; kept so existing power JSON keeps working. */
	public static final String FABRIC_CONDITIONS_KEY = "fabric:load_conditions";

	// NeoForge port: Fabric ResourceConditionType -> NeoForge condition codec registry.
	// Apoli's mod constructor must call register(modBus) (replaces ResourceConditions.register(...)).
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Apoli.MODID);

	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NamespacesLoadedResourceCondition>> ANY_NAMESPACE_LOADED = CONDITION_CODECS.register("any_namespace_loaded", () -> NamespacesLoadedResourceCondition.ANY_CODEC);

	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NamespacesLoadedResourceCondition>> ALL_NAMESPACES_LOADED = CONDITION_CODECS.register("all_namespaces_loaded", () -> NamespacesLoadedResourceCondition.ALL_CODEC);

	public static void register(IEventBus modBus) {
		CONDITION_CODECS.register(modBus);
	}

	public static class NamespacesLoadedResourceCondition implements ICondition {
		public static final MapCodec<NamespacesLoadedResourceCondition> ANY_CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
				Codec.STRING.listOf()
					.fieldOf("namespaces")
					.forGetter(e -> e.namespaces)
			)
				.apply(instance, namespaces -> new NamespacesLoadedResourceCondition(namespaces, false))
		);

		public static final MapCodec<NamespacesLoadedResourceCondition> ALL_CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(
					Codec.STRING.listOf()
						.fieldOf("namespaces")
						.forGetter(e -> e.namespaces)
				)
				.apply(instance, namespaces -> new NamespacesLoadedResourceCondition(namespaces, true))
		);

		private final List<String> namespaces;
		private final boolean and;

		public NamespacesLoadedResourceCondition(List<String> namespaces, boolean and) {
			this.namespaces = namespaces;
			this.and = and;
		}

		@Override
		public MapCodec<? extends ICondition> codec() {
			return and ? ALL_CODEC : ANY_CODEC;
		}

		@Override
		public boolean test(IContext context) {
			for (String namespace : namespaces) {
				if (PowerTypes.LOADED_NAMESPACES.contains(namespace) != and) {
					return !and;
				}
			}

			return and;
		}
	}

	public static boolean namespacesLoaded(JsonObject jsonObject, Set<String> namespaces, boolean and) {

		JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "namespaces");
		for (JsonElement jsonElement : jsonArray) {
			if (jsonElement.isJsonPrimitive()) {
				if (namespaces.contains(jsonElement.getAsString()) != and) {
					return !and;
				}
			} else {
				throw new JsonParseException("Invalid " + jsonElement + " entry: expected a JSON string!");
			}
		}

		return and;

	}

	/**
	 * Tests a power file's load conditions. Accepts the legacy Fabric layout
	 * ({@code "fabric:load_conditions": [{"condition": id, ...}]}) and NeoForge's
	 * ({@code "neoforge:conditions": [{"type": id, ...}]}). Conditions are resolved through
	 * NeoForge's condition codec registry, so Apoli's and NeoForge's built-in conditions work;
	 * Fabric-only condition ids (e.g. fabric:all_mods_loaded) are unknown and fail the file, as before.
	 */
	public static boolean test(Identifier id, JsonObject jsonObject) {

		try {
			List<ICondition> parsedConditions = new ArrayList<>();
			parse(jsonObject, FABRIC_CONDITIONS_KEY, "condition", parsedConditions);
			parse(jsonObject, ConditionalOps.DEFAULT_CONDITIONS_KEY, "type", parsedConditions);
			for (ICondition condition : parsedConditions) {
				if (!condition.test(ICondition.IContext.TAGS_INVALID)) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException e) {
			Apoli.LOGGER.error("There was a problem parsing the resource condition(s) of power file " + id + " (skipping): " + e.getMessage());
			return false;
		}

	}

	private static void parse(JsonObject jsonObject, String key, String typeField, List<ICondition> out) {
		JsonArray conditions = GsonHelper.getAsJsonArray(jsonObject, key, null);
		if (conditions == null) {
			return;
		}
		for (JsonElement condition : conditions) {
			JsonObject json = condition.getAsJsonObject().deepCopy();
			Identifier conditionId = Identifier.parse(json.remove(typeField).getAsString());
			MapCodec<? extends ICondition> codec = NeoForgeRegistries.CONDITION_SERIALIZERS.getValue(conditionId);
			if (codec == null) {
				throw new JsonParseException("Unknown resource condition " + conditionId);
			}
			out.add(codec.codec().parse(JsonOps.INSTANCE, json).getOrThrow());
		}
	}

}
