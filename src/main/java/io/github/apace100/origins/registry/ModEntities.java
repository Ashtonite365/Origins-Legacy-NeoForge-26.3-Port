package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.entity.EnderianPearlEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Origins.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EnderianPearlEntity>> ENDERIAN_PEARL = ENTITY_TYPES.register("enderian_pearl",
        id -> EntityType.Builder.<EnderianPearlEntity>of(EnderianPearlEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(64)
            .updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
