package io.github.apace100.apoli.registry;

import com.mojang.datafixers.util.Pair;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.util.modifier.IModifierOperation;
import io.github.apace100.calio.ClassUtil;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.tuple.Triple;

public class ApoliRegistries {

    public static final Registry<PowerFactory> POWER_FACTORY;
    public static final Registry<ConditionFactory<Entity>> ENTITY_CONDITION;
    public static final Registry<ConditionFactory<Pair<Entity, Entity>>> BIENTITY_CONDITION;
    public static final Registry<ConditionFactory<ItemStack>> ITEM_CONDITION;
    public static final Registry<ConditionFactory<BlockInWorld>> BLOCK_CONDITION;
    public static final Registry<ConditionFactory<Pair<DamageSource, Float>>> DAMAGE_CONDITION;
    public static final Registry<ConditionFactory<FluidState>> FLUID_CONDITION;
    public static final Registry<ConditionFactory<Holder<Biome>>> BIOME_CONDITION;
    public static final Registry<ActionFactory<Entity>> ENTITY_ACTION;
    public static final Registry<ActionFactory<Pair<Level, ItemStack>>> ITEM_ACTION;
    public static final Registry<ActionFactory<Triple<Level, BlockPos, Direction>>> BLOCK_ACTION;
    public static final Registry<ActionFactory<Pair<Entity, Entity>>> BIENTITY_ACTION;
    public static final Registry<IModifierOperation> MODIFIER_OPERATION;

    static {
        POWER_FACTORY = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("power_factory")));
        ENTITY_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("entity_condition")));
        BIENTITY_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("bientity_condition")));
        ITEM_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("item_condition")));
        BLOCK_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("block_condition")));
        DAMAGE_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("damage_condition")));
        FLUID_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("fluid_condition")));
        BIOME_CONDITION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("biome_condition")));
        ENTITY_ACTION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("entity_action")));
        ITEM_ACTION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("item_action")));
        BLOCK_ACTION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("block_action")));
        BIENTITY_ACTION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("bientity_action")));
        MODIFIER_OPERATION = PlatformRegistries.createSimple(ResourceKey.createRegistryKey(Apoli.identifier("modifier_operation")));
    }
}
