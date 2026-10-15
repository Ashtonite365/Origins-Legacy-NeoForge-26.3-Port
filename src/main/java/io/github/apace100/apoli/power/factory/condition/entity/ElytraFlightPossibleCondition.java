package io.github.apace100.apoli.power.factory.condition.entity;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.PreventElytraFlightPower;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraFlightPossibleCondition {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if(!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }
        boolean ability = true;
        if(data.getBoolean("check_ability")) {
            ItemStack equippedChestItem = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
            ability = equippedChestItem.is(Items.ELYTRA) && LivingEntity.canGlideUsing(equippedChestItem, EquipmentSlot.CHEST);
            // NeoForge port: Fabric's EntityElytraEvents.CUSTOM -> any glider item in any slot or the GLIDING_FLIGHT attribute.
            if (!ability && io.github.apace100.apoli.power.ElytraFlightPower.grantsElytraFlight(livingEntity)) ability = true;
            if (!ability) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    if (LivingEntity.canGlideUsing(livingEntity.getItemBySlot(slot), slot)) { ability = true; break; }
                }
                if (!ability && livingEntity.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.GLIDING_FLIGHT) > 0) ability = true;
            }
            // Fabric's EntityElytraEvents.ALLOW was only fed by PreventElytraFlightPower.
            if (PowerHolderComponent.hasPower(livingEntity, PreventElytraFlightPower.class)) {
                ability = false;
            }
        }
        boolean state = true;
        if(data.getBoolean("check_state")) {
            state = !livingEntity.onGround() && !livingEntity.isFallFlying() && !livingEntity.isInWater() && !livingEntity.hasEffect(MobEffects.LEVITATION);
        }
        return ability && state;
    }

    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(Apoli.identifier("elytra_flight_possible"),
            new SerializableData()
                .add("check_state", SerializableDataTypes.BOOLEAN, false)
                .add("check_ability", SerializableDataTypes.BOOLEAN, true),
            ElytraFlightPossibleCondition::condition
        );
    }
}