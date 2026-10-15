/*
 * Origins: Legacy personal NeoForge port. MIT licensed (see LICENSE-apoli).
 * Re-implements AdditionalEntityAttributes 2.2.4's lava_speed hook (MIT, Copyright (c) DaFuqs; neoforge LivingEntityMixin),
 * since AEA has no NeoForge 26.3 build.
 */
package io.github.apace100.apoli.mixin;

import de.dafuqs.additionalentityattributes.AdditionalEntityAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LavaAttributesMixin {
    /**
     * AEA 2.2.4: the lava velocity damping factor (vanilla {@code getDeltaMovement().scale(0.5)}) becomes the
     * lava_speed attribute value. The attribute's base is kept in sync with vanilla's 0.5, so modifiers act on it.
     */
    @ModifyArg(method = "travelInLava", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private double apoli$lavaSpeed(double original) {
        AttributeInstance lavaSpeed = ((LivingEntity) (Object) this).getAttribute(AdditionalEntityAttributes.LAVA_SPEED);
        if (lavaSpeed == null) {
            return original;
        }
        if (lavaSpeed.getBaseValue() != original) {
            lavaSpeed.setBaseValue(original);
        }
        return lavaSpeed.getValue();
    }
}
