package io.github.apace100.apoli.power;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Consumer;

public class PreventElytraFlightPower extends Power {

    private final Consumer<Entity> entityAction;

    public PreventElytraFlightPower(PowerType<?> type, LivingEntity entity, Consumer<Entity> entityAction) {
        super(type, entity);
        this.entityAction = entityAction;
    }

    public void executeAction(Entity entity) {
        if(entityAction != null) {
            entityAction.accept(entity);
        }
    }

    /**
     * NeoForge port: replaces Fabric's EntityElytraEvents.ALLOW listener. The platform layer / elytra mixin
     * must deny elytra flight when this returns false.
     */
    public static boolean allowsElytraFlight(net.minecraft.world.entity.LivingEntity entity) {
        return !PowerHolderComponent.hasPower(entity, PreventElytraFlightPower.class);
    }

    public static PowerFactory createFactory() {
        return new PowerFactory<>(Apoli.identifier("prevent_elytra_flight"),
            new SerializableData()
                .add("entity_action", ApoliDataTypes.ENTITY_ACTION, null),
            data ->
                (type, player) -> new PreventElytraFlightPower(type, player, data.get("entity_action")))
            .allowCondition();
    }
}
