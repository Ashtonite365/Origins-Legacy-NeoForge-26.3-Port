package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.command.LayerArgumentType;
import io.github.apace100.origins.command.OriginArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;

/** NeoForge port: replaces the dropped ArgumentTypeInfos mixin. Called from OriginsLegacyMod during mod construction. */
public final class ModArgumentTypes {
    private ModArgumentTypes() {}

    public static void register() {
        PlatformRegistries.register(Registries.COMMAND_ARGUMENT_TYPE, Origins.identifier("origin"),
            ArgumentTypeInfos.registerByClass(OriginArgumentType.class, SingletonArgumentInfo.contextFree(OriginArgumentType::origin)));
        PlatformRegistries.register(Registries.COMMAND_ARGUMENT_TYPE, Origins.identifier("layer"),
            ArgumentTypeInfos.registerByClass(LayerArgumentType.class, SingletonArgumentInfo.contextFree(LayerArgumentType::layer)));
    }
}
