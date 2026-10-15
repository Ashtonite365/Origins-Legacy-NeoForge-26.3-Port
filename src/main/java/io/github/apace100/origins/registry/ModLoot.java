package io.github.apace100.origins.registry;

import com.mojang.serialization.MapCodec;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.util.OriginLootCondition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ModLoot {

    private static final Identifier DUNGEON_LOOT = Identifier.withDefaultNamespace("chests/simple_dungeon");
    private static final Identifier STRONGHOLD_LIBRARY = Identifier.withDefaultNamespace("chests/stronghold_library");
    private static final Identifier MINESHAFT = Identifier.withDefaultNamespace("chests/abandoned_mineshaft");
    private static final Identifier WATER_RUIN = Identifier.withDefaultNamespace("chests/underwater_ruin_small");

    public static final MapCodec<? extends LootItemCondition> ORIGIN_LOOT_CONDITION = registerLootCondition("origin", OriginLootCondition.CODEC);

    private static MapCodec<? extends LootItemCondition> registerLootCondition(String path, MapCodec<? extends LootItemCondition> serializer) {
        return PlatformRegistries.register(Registries.LOOT_CONDITION_TYPE, Origins.identifier(path), serializer);
    }

    public static void registerLootTables() {
        // NeoForge port: Fabric LootTableEvents.MODIFY -> LootTableLoadEvent (vanilla tables only, as before).
        NeoForge.EVENT_BUS.addListener(LootTableLoadEvent.class, event -> {
            var resourceKey = event.getKey();
            var tableBuilder = event.getTable();
            var registries = event.getRegistries();
            if (!resourceKey.identifier().getNamespace().equals("minecraft")) {
                return;
            }
            var waterProtection = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.WATER_PROTECTION);
            var identifier = resourceKey.identifier();
            if (DUNGEON_LOOT.equals(identifier)) {
                LootPool.Builder lootPool = new LootPool.Builder();
                lootPool.setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(20)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(1))))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(10)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(2))))
                    .add(EmptyLootItem.emptyItem().setWeight(80));
                tableBuilder.addPool(lootPool.build());
            } else if (STRONGHOLD_LIBRARY.equals(identifier)) {
                LootPool.Builder lootPool = new LootPool.Builder();
                lootPool.setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(20)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(2))))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(10)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(3))))
                    .add(EmptyLootItem.emptyItem().setWeight(80));
                tableBuilder.addPool(lootPool.build());
            } else if (MINESHAFT.equals(identifier)) {
                LootPool.Builder lootPool = new LootPool.Builder();
                lootPool.setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(20)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(1))))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(5)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(2))))
                    .add(EmptyLootItem.emptyItem().setWeight(90));
                tableBuilder.addPool(lootPool.build());
            } else if (WATER_RUIN.equals(identifier)) {
                LootPool.Builder lootPool = new LootPool.Builder();
                lootPool.setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(10)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(1))))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .setWeight(20)
                        .apply(new SetEnchantmentsFunction.Builder().withEnchantment(waterProtection, ContextIntProviders.exactly(2))))
                    .add(EmptyLootItem.emptyItem().setWeight(110));
                tableBuilder.addPool(lootPool.build());
            }
        });
    }
}
