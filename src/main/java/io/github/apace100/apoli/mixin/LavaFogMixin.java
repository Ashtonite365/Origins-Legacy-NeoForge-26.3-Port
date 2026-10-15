/*
 * Origins: Legacy personal NeoForge port. MIT licensed (see LICENSE-apoli).
 * Re-implements AdditionalEntityAttributes 2.2.4's lava_visibility hook (MIT, Copyright (c) DaFuqs; client LavaFogEnvironmentMixin)
 * on the 26.3 LavaFogEnvironment: same constants and formulas.
 */
package io.github.apace100.apoli.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.dafuqs.additionalentityattributes.AdditionalEntityAttributes;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogMixin {

	private static AttributeInstance additionalEntityAttributes$lavaVisibility(Camera camera, float original) {
		AttributeInstance attribute = camera.entity() instanceof LivingEntity living ? living.getAttribute(AdditionalEntityAttributes.LAVA_VISIBILITY) : null;
		if (attribute != null && attribute.getBaseValue() != original) {
			attribute.setBaseValue(original);
		}
		return attribute;
	}

	// No fire resistance: start 0.25 / end 1.0
	@ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=0.25F", ordinal = 0))
	private float additionalEntityAttributes$minWithoutFireResistance(float original, @Local(argsOnly = true) Camera camera) {
		AttributeInstance a = additionalEntityAttributes$lavaVisibility(camera, original);
		return a == null ? original : original - (float) a.getValue() * 0.25F;
	}

	@ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=1.0F", ordinal = 0))
	private float additionalEntityAttributes$maxWithoutFireResistance(float original, @Local(argsOnly = true) Camera camera) {
		AttributeInstance a = additionalEntityAttributes$lavaVisibility(camera, original);
		return a == null ? original : (float) a.getValue();
	}

	// Fire resistance: start 0.0 / end 5.0
	@ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=0.0F", ordinal = 0))
	private float additionalEntityAttributes$minFireResistance(float original, @Local(argsOnly = true) Camera camera) {
		AttributeInstance a = additionalEntityAttributes$lavaVisibility(camera, original);
		return a == null ? original : original - (float) a.getValue();
	}

	@ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=5.0F", ordinal = 0))
	private float additionalEntityAttributes$maxFireResistance(float original, @Local(argsOnly = true) Camera camera) {
		AttributeInstance a = additionalEntityAttributes$lavaVisibility(camera, original);
		return a == null ? original : (float) a.getValue();
	}
}
