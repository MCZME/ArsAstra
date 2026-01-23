package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.github.mczme.arsastra.client.model.CopperTunModel;
import com.github.mczme.arsastra.core.starchart.engine.StarChartContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.alchemy.PotionContents;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.ArrayList;
import java.util.List;

public class CopperTunRenderer extends GeoBlockRenderer<CopperTunBlockEntity> {
    public CopperTunRenderer(BlockEntityRendererProvider.Context context) {
        super(new CopperTunModel());
    }

    @Override
    public void render(CopperTunBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 渲染液面
        if (animatable.getFluidLevel() > 0) {
            renderFluid(animatable, poseStack, bufferSource, packedLight);
        }
    }

    /**
     * 绘制釜内液面的核心方法
     */
    private void renderFluid(CopperTunBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();

        // 计算高度 (下移 2 个像素, 1 像素 = 0.0625f)
        float y = 0.2f + (entity.getFluidLevel() * 0.25f) - 0.125f;
        
        // 确定颜色
        int color = 0xFF3F76E4; // 默认水色
        StarChartContext context = entity.getContext();
        if (context != null && !context.predictedEffects().isEmpty()) {
            List<MobEffectInstance> effects = new ArrayList<>();
            context.predictedEffects().forEach((field, data) -> {
                BuiltInRegistries.MOB_EFFECT.getHolder(field.effect()).ifPresent(holder -> {
                    effects.add(new MobEffectInstance(holder, data.duration(), data.level()));
                });
            });
            if (!effects.isEmpty()) {
                color = PotionContents.getColor(effects);
            }
        } else if (entity.getFluidType().getPath().contains("lava")) {
            color = 0xFFFF4500;
        }

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.8f;

        // 获取正确的流体纹理 Sprite
        ResourceLocation fluidLoc;
        if (entity.getFluidType().getPath().contains("lava")) {
            fluidLoc = ResourceLocation.withDefaultNamespace("block/lava_still");
        } else {
            fluidLoc = ResourceLocation.withDefaultNamespace("block/water_still");
        }
        
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidLoc);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        float min = 0.125f;
        float max = 0.875f;

        // 使用 Sprite 的 UV 坐标
        vertexConsumer.addVertex(matrix, min, y, min).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV0()).setLight(light).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, min, y, max).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV1()).setLight(light).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, max, y, max).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV1()).setLight(light).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, max, y, min).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV0()).setLight(light).setNormal(0, 1, 0);

        poseStack.popPose();
    }
}