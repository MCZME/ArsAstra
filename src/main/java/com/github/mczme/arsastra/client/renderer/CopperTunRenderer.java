package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.github.mczme.arsastra.client.model.CopperTunModel;
import com.github.mczme.arsastra.core.starchart.engine.StarChartContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public class CopperTunRenderer extends GeoBlockRenderer<CopperTunBlockEntity> {
    public CopperTunRenderer(BlockEntityRendererProvider.Context context) {
        super(new CopperTunModel());
    }

    @Override
    public void render(CopperTunBlockEntity animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 渲染指引 (位于最上方)
        renderGuide(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 渲染搅拌棒 (先渲染实体)
        renderStirringStick(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        // 渲染液面 (后渲染半透明)
        if (animatable.getFluidLevel() > 0) {
            renderFluid(animatable, poseStack, bufferSource, packedLight);
        }
    }

    private void renderStirringStick(CopperTunBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        ItemStack stick = entity.getStirringStick();
        if (stick.isEmpty()) return;

        poseStack.pushPose();

        // 1. 基础定位：釜口中心上方
        poseStack.translate(0.5, 0.7, 0.5);

        // 2. 搅拌动画 (公转)
        float angle = 0;
        if (entity.isStirring()) {
             // 速度: 1圈/20ticks
             float time = entity.clientStirAnim + partialTick;
             angle = time * 18.0f;
             if (!entity.isStirringClockwise()) angle = -angle;
        }
        
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        // 3. 偏心 (绕着 Y 轴旋转后，向 Z 轴平移，实现公转)
        poseStack.translate(0, -0.1, 0.18); 

        // 4. 自身姿态 (翻转并倾斜插入)
        poseStack.mulPose(Axis.ZP.rotationDegrees(100)); // 修复头朝下的问题
        poseStack.mulPose(Axis.XP.rotationDegrees(-20)); // 向外倾斜
        poseStack.scale(1.2f, 1.2f, 1.2f);

        // 5. 渲染物品 (强制立即绘制以确保深度顺序)
        MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(stick, ItemDisplayContext.FIXED, light, overlay, poseStack, immediate, entity.getLevel(), 0);
        immediate.endBatch();

        poseStack.popPose();
    }

    @SuppressWarnings("null")
    private void renderGuide(CopperTunBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (entity.guidedSequence.isEmpty() || entity.guideIndex >= entity.guidedSequence.size()) return;

        poseStack.pushPose();
        
        // 1. 基础定位：釜中心上方
        float time = (entity.getLevel().getGameTime() % 1000) + partialTick;
        float bobbing = (float) Math.sin(time * 0.1f) * 0.05f;
        poseStack.translate(0.5, 1.1 + bobbing, 0.5);

        if (entity.isWaitingForItem) {
            // --- 渲染幽灵物品 ---
            ItemStack targetStack = entity.guidedSequence.get(entity.guideIndex).stack();
            if (!targetStack.isEmpty()) {
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.0f));
                poseStack.scale(0.6f, 0.6f, 0.6f);
                
                // 使用特殊的半透明 BufferSource
                Minecraft.getInstance().getItemRenderer().renderStatic(targetStack, ItemDisplayContext.GROUND, light, overlay, poseStack, buffer, entity.getLevel(), 0);
            }
        } else {
            // --- 渲染搅拌指引 ---
            // 直接用搅拌棒物品代表搅拌操作
            ItemStack stick = new ItemStack(com.github.mczme.arsastra.registry.AAItems.STIRRING_STICK.get());
            boolean clockwise = entity.currentGuideRotation > 0;
            
            poseStack.mulPose(Axis.YP.rotationDegrees(clockwise ? time * 5.0f : -time * 5.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45)); // 倾斜
            poseStack.scale(0.8f, 0.8f, 0.8f);

            Minecraft.getInstance().getItemRenderer().renderStatic(stick, ItemDisplayContext.FIXED, light, overlay, poseStack, buffer, entity.getLevel(), 0);
            
            // 渲染一个简单的方向文字或箭头 (可选)
        }

        poseStack.popPose();
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