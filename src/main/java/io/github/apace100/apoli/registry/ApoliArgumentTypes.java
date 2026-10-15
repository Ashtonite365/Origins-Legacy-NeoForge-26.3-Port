package io.github.apace100.apoli.registry;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.command.PowerOperation;
import io.github.apace100.apoli.command.PowerTypeArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;

/** NeoForge port: replaces the dropped ArgumentTypeInfos mixin. Called from OriginsLegacyMod during mod construction. */
public final class ApoliArgumentTypes {
    private ApoliArgumentTypes() {}

    public static void register() {
        PlatformRegistries.register(Registries.COMMAND_ARGUMENT_TYPE, Apoli.identifier("power"),
            ArgumentTypeInfos.registerByClass(PowerTypeArgumentType.class, SingletonArgumentInfo.contextFree(PowerTypeArgumentType::power)));
        PlatformRegistries.register(Registries.COMMAND_ARGUMENT_TYPE, Apoli.identifier("power_operation"),
            ArgumentTypeInfos.registerByClass(PowerOperation.class, SingletonArgumentInfo.contextFree(PowerOperation::operation)));
    }
}
