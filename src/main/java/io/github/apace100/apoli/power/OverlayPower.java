package io.github.apace100.apoli.power;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import static io.github.apace100.apoli.power.OverlayPowerPipelines.NAUSEA_PIPELINE;
import static io.github.apace100.apoli.power.OverlayPowerPipelines.OVERLAY_PIPELINE;

public class OverlayPower extends Power {
    /* package-private for OverlayPowerRenderer */ final Identifier texture;
    /* package-private for OverlayPowerRenderer */ final float strength;
    /* package-private for OverlayPowerRenderer */ final float red;
    /* package-private for OverlayPowerRenderer */ final float green;
    /* package-private for OverlayPowerRenderer */ final float blue;
    /* package-private for OverlayPowerRenderer */ final DrawMode drawMode;
    private final DrawPhase drawPhase;
    private final boolean hideWithHud;
    private final boolean visibleInThirdPerson;

    public enum DrawMode {
        NAUSEA, TEXTURE
    }

    public enum DrawPhase {
        BELOW_HUD, ABOVE_HUD
    }

    public OverlayPower(PowerType<?> type, LivingEntity entity, Identifier texture, float strength, float red, float green, float blue, DrawMode drawMode, DrawPhase drawPhase, boolean hideWithHud, boolean visibleInThirdPerson) {
        super(type, entity);
        this.texture = texture;
        this.strength = strength;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.drawMode = drawMode;
        this.drawPhase = drawPhase;
        this.hideWithHud = hideWithHud;
        this.visibleInThirdPerson = visibleInThirdPerson;
    }

    public DrawPhase getDrawPhase() {
        return drawPhase;
    }

    public boolean shouldBeVisibleInThirdPerson() {
        return visibleInThirdPerson;
    }

    public boolean doesHideWithHud() {
        return hideWithHud;
    }

    public void render(GuiGraphicsExtractor guiGraphics) {
        // NeoForge port: client-only rendering lives in OverlayPowerRenderer so loading this class on a dedicated server
        // does not drag in client classes (the verifier resolved the anonymous GuiElementRenderState here).
        OverlayPowerRenderer.render(this, guiGraphics);
    }

    public static PowerFactory createFactory() {
        return new PowerFactory<>(Apoli.identifier("overlay"),
            new SerializableData()
                .add("texture", SerializableDataTypes.IDENTIFIER)
                .add("strength", SerializableDataTypes.FLOAT, 1.0F)
                .add("red", SerializableDataTypes.FLOAT, 1.0F)
                .add("green", SerializableDataTypes.FLOAT, 1.0F)
                .add("blue", SerializableDataTypes.FLOAT, 1.0F)
                .add("draw_mode", SerializableDataType.enumValue(OverlayPower.DrawMode.class))
                .add("draw_phase", SerializableDataType.enumValue(OverlayPower.DrawPhase.class))
                .add("hide_with_hud", SerializableDataTypes.BOOLEAN, true)
                .add("visible_in_third_person", SerializableDataTypes.BOOLEAN, false),
            data ->
                (type, player) -> new OverlayPower(type, player,
                    data.getId("texture"),
                    data.getFloat("strength"),
                    data.getFloat("red"),
                    data.getFloat("green"),
                    data.getFloat("blue"),
                    (OverlayPower.DrawMode) data.get("draw_mode"),
                    (OverlayPower.DrawPhase) data.get("draw_phase"),
                    data.getBoolean("hide_with_hud"),
                    data.getBoolean("visible_in_third_person")))
            .allowCondition();
    }
}
