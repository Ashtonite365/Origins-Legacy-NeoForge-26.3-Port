/*
 * Origins: Legacy personal NeoForge port. MIT licensed (see LICENSE-apoli).
 * Replaces Fabric API's EntityElytraEvents.ALLOW / CUSTOM hooks for PreventElytraFlightPower and ElytraFlightPower.
 * This mixin is the single place elytra flight is granted/denied (H1 does not add an event for it).
 */
package io.github.apace100.apoli.mixin;

import io.github.apace100.apoli.power.ElytraFlightPower;
import io.github.apace100.apoli.power.PreventElytraFlightPower;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ElytraDenyMixin {
    // LivingEntity#canGlide() and LocalPlayer#canGlide() delegate here; Player#canGlide() calls super.canGlide(),
    // so starting (Player#tryToStartFallFlying) and keeping (LivingEntity#updateFallFlying) a glide are both covered.
    @Inject(method = "canGlide(Z)Z", at = @At("RETURN"), cancellable = true)
    private void apoli$elytraFlightPowers(boolean isNeoForgeConnection, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean result = cir.getReturnValueZ();
        if (!result && ElytraFlightPower.grantsElytraFlight(self)
            && !self.onGround() && !self.isPassenger() && !self.hasEffect(MobEffects.LEVITATION)) {
            result = true; // same preconditions as vanilla, but no glider item needed
        }
        if (result && !PreventElytraFlightPower.allowsElytraFlight(self)) {
            result = false; // deny wins over grant
        }
        if (result != cir.getReturnValueZ()) cir.setReturnValue(result);
    }
}
