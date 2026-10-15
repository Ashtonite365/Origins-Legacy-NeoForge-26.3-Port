package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.TemporaryCobwebBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge: blocks are built lazily inside RegisterEvent (vanilla registries are frozen at class-init time). */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Origins.MODID);

    public static final DeferredHolder<Block, TemporaryCobwebBlock> TEMPORARY_COBWEB = BLOCKS.register("temporary_cobweb",
        id -> new TemporaryCobwebBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL).forceSolidOn().noCollision().requiresCorrectToolForDrops().strength(4.0F)
            .setId(ResourceKey.create(Registries.BLOCK, id))));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
