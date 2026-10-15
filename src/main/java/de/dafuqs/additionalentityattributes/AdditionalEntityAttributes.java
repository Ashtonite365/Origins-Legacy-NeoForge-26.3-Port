/*
 * NeoForge 26.3 stand-in for Additional Entity Attributes 2.2.4 (MIT, Copyright (c) DaFuqs) for the private Origins: Legacy port.
 * Ids, translation keys, base/min/max values and entity attachment match AEA 2.2.4 (26.2-multiloader branch).
 * Exception (by design): WATER_SPEED is NeoForge's built-in swim speed attribute; the AEA id
 * additionalentityattributes:water_speed is registered as an alias of it.
 * Gameplay hooks ported so far: LAVA_SPEED (mixin.LivingEntityLavaSpeedMixin), LAVA_VISIBILITY (mixin.client.LavaFogEnvironmentMixin).
 */
package de.dafuqs.additionalentityattributes;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class AdditionalEntityAttributes {
	public static final String MOD_ID = "additionalentityattributes";
	public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MOD_ID);

	public static final DeferredHolder<Attribute, Attribute> CRITICAL_BONUS_DAMAGE = register("critical_bonus_damage", 0.5, -1.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> WATER_VISIBILITY = register("water_visibility", 96.0, 0.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> LAVA_SPEED = register("lava_speed", 0.5, 0.0, 1.0);
	public static final DeferredHolder<Attribute, Attribute> WIDTH = register("width", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> HEIGHT = register("height", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> HITBOX_SCALE = register("hitbox_scale", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> HITBOX_WIDTH = register("hitbox_width", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> HITBOX_HEIGHT = register("hitbox_height", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> MODEL_SCALE = register("model_scale", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> MODEL_WIDTH = register("model_width", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> MODEL_HEIGHT = register("model_height", 1.0, 0.0625, 16.0);
	public static final DeferredHolder<Attribute, Attribute> MOB_DETECTION_RANGE = register("mob_detection_range", 0.0, 0.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> LAVA_VISIBILITY = register("lava_visibility", 1.0, 0.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> BONUS_LOOT_COUNT_ROLLS = register("bonus_loot_count_rolls", 0.0, 0.0, 128.0);
	public static final DeferredHolder<Attribute, Attribute> BONUS_RARE_LOOT_ROLLS = register("bonus_rare_loot_rolls", 0.0, 0.0, 128.0);
	public static final DeferredHolder<Attribute, Attribute> DROPPED_EXPERIENCE = register("dropped_experience", 1.0, 0.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> MAGIC_PROTECTION = register("magic_protection", 0.0, 0.0, 1024.0);
	public static final DeferredHolder<Attribute, Attribute> COLLECTION_RANGE = register("collection_range", 0.0, -64.0, 64.0);

	/** AEA water_speed -> NeoForge swim speed (kept by design). */
	public static final Holder<Attribute> WATER_SPEED = NeoForgeMod.SWIM_SPEED;

	private AdditionalEntityAttributes() {}

	private static DeferredHolder<Attribute, Attribute> register(String name, double base, double min, double max) {
		return ATTRIBUTES.register(name, () -> new RangedAttribute("attribute.name." + MOD_ID + '.' + name, base, min, max).setSyncable(true));
	}

	public static void register(IEventBus modBus) {
		ATTRIBUTES.register(modBus);
		modBus.addListener(AdditionalEntityAttributes::addToEntities);
		modBus.addListener(EventPriority.LOWEST, (RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.ATTRIBUTE)) {
				BuiltInRegistries.ATTRIBUTE.addAlias(Identifier.fromNamespaceAndPath(MOD_ID, "water_speed"), WATER_SPEED.unwrapKey().orElseThrow().identifier());
			}
		});
	}

	/** Same attachment as AEA 2.2.4's NeoForge entrypoint (swim speed is already on every living entity). */
	private static void addToEntities(EntityAttributeModificationEvent event) {
		for (EntityType<? extends LivingEntity> type : event.getTypes()) {
			add(event, type, LAVA_SPEED);
			add(event, type, WIDTH);
			add(event, type, HEIGHT);
			add(event, type, HITBOX_SCALE);
			add(event, type, HITBOX_WIDTH);
			add(event, type, HITBOX_HEIGHT);
			add(event, type, MODEL_SCALE);
			add(event, type, MODEL_WIDTH);
			add(event, type, MODEL_HEIGHT);
			add(event, type, MOB_DETECTION_RANGE);
			add(event, type, MAGIC_PROTECTION);
		}
		add(event, EntityTypes.PLAYER, WATER_VISIBILITY);
		add(event, EntityTypes.PLAYER, LAVA_VISIBILITY);
		add(event, EntityTypes.PLAYER, CRITICAL_BONUS_DAMAGE);
		add(event, EntityTypes.PLAYER, BONUS_LOOT_COUNT_ROLLS);
		add(event, EntityTypes.PLAYER, BONUS_RARE_LOOT_ROLLS);
		add(event, EntityTypes.PLAYER, DROPPED_EXPERIENCE);
		add(event, EntityTypes.PLAYER, COLLECTION_RANGE);
	}

	private static void add(EntityAttributeModificationEvent event, EntityType<? extends LivingEntity> type, Holder<Attribute> attribute) {
		if (!event.has(type, attribute)) event.add(type, attribute);
	}
}
