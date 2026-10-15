package io.github.apace100.apoli.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.ModifyInsomniaTicksPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Unique
    private Player apoli$CachedPlayer;

    // NeoForge patch: the per-player hasSkyLight() call is gone; cache the loop player at isSpectator() instead.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z", ordinal = 0))
    private void cachePlayerEntity(ServerLevel level, boolean spawnEnemies, CallbackInfo ci, @Local ServerPlayer serverPlayerEntity) {
        apoli$CachedPlayer = serverPlayerEntity;
    }

    @ModifyVariable(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1), ordinal = 0)
    private int modifyTicks(int original) {
        return (int)PowerHolderComponent.modify(apoli$CachedPlayer, ModifyInsomniaTicksPower.class, original);
    }
}
