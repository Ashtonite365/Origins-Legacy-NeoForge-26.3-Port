/*
 * Origins: Legacy personal NeoForge port. MIT licensed (see LICENSE-apoli). Client-only half of OverlayPower.
 */
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

final class OverlayPowerRenderer {
    private OverlayPowerRenderer() {}

    static void render(OverlayPower power, GuiGraphicsExtractor guiGraphics) {
        Minecraft client = Minecraft.getInstance();
        int i = client.getWindow().getGuiScaledWidth();
        int j = client.getWindow().getGuiScaledHeight();

        float d, e, l, m, n;
        float g, h, k, a;

        switch(power.drawMode) {
            case NAUSEA:
                d = Mth.lerp(power.strength, 2.0f, 1.0f);
                g = power.red * power.strength;
                h = power.green * power.strength;
                k = power.blue * power.strength;
                e = i * d;
                l = j * d;
                m = (i - e) / 2.0f;
                n = (j - l) / 2.0f;
                a = 1.0F;
                break;
            case TEXTURE: default:
                g = power.red;
                h = power.green;
                k = power.blue;
                a = power.strength;
                e = i;
                l = j;
                m = 0;
                n = 0;
                break;
        }

        guiGraphics.blit(RenderPipelines.GUI_NAUSEA_OVERLAY, power.texture, 0, 0, 0f, 0f, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), ARGB.colorFromFloat(a, g, h, k));

        guiGraphics.guiRenderState.addGuiElement(new GuiElementRenderState() {
            @Override
            public void buildVertices(VertexConsumer vertexConsumer) {
                vertexConsumer.addVertex(m, n + l, -90.0f).setUv(0.0F, 1.0F).setColor(g, h, k, a);
                vertexConsumer.addVertex(m + e, n + l, -90.0f).setUv(1.0F, 1.0F).setColor(g, h, k, a);
                vertexConsumer.addVertex(m + e, n, -90.0f).setUv(1.0F, 0.0F).setColor(g, h, k, a);
                vertexConsumer.addVertex(m, n, -90.0f).setUv(0.0F, 0.0F).setColor(g, h, k, a);
            }

            @Override
            public RenderPipeline pipeline() {
                if (power.drawMode == OverlayPower.DrawMode.NAUSEA) {
                    return NAUSEA_PIPELINE;
                } else {
                    return OVERLAY_PIPELINE;
                }
            }

            @Override
            public TextureSetup textureSetup() {
                var clientTexture = client.getTextureManager().getTexture(power.texture);
                var gpuTextureView = clientTexture.getTextureView();
                var gpuSampler = clientTexture.getSampler();
                return TextureSetup.singleTexture(gpuTextureView, gpuSampler);
            }

            @Override
            public @Nullable ScreenRectangle scissorArea() {
                return null;
            }

            @Override
            public @Nullable ScreenRectangle bounds() {
                return null;
            }
        });
    }
}
