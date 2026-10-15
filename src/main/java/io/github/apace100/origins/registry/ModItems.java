package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.content.OrbOfOriginItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge: items are built lazily inside RegisterEvent; OrbOfOriginItem sets its own ResourceKey id. */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Origins.MODID);

    public static final DeferredHolder<Item, OrbOfOriginItem> ORB_OF_ORIGIN = ITEMS.register("orb_of_origin", OrbOfOriginItem::new);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
