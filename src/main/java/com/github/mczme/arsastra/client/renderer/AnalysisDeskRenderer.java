package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.block.AnalysisDeskBlock;
import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class AnalysisDeskRenderer implements BlockEntityRenderer<AnalysisDeskBlockEntity> {
    private final ItemRenderer itemRenderer;

    public AnalysisDeskRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(AnalysisDeskBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack stack = blockEntity.getItemHandler().getStackInSlot(0);
        if (stack.isEmpty()) return;

        Direction facing = blockEntity.getBlockState().getValue(AnalysisDeskBlock.FACING);

        poseStack.pushPose();

        // 1. 定位到标本台中心
        // 尝试调整到左上角
        double offsetX = 0.5;
        double offsetZ = 0.5;
        double shiftSide = -0.18;    // 向左偏移
        double shiftForward = 0.13; // 向前/上偏移

        switch (facing) {
            case NORTH -> { offsetX += shiftSide; offsetZ += shiftForward; }
            case SOUTH -> { offsetX -= shiftSide; offsetZ -= shiftForward; }
            case WEST ->  { offsetZ -= shiftSide; offsetX += shiftForward; }
            default ->  { offsetZ += shiftSide; offsetX -= shiftForward; }
        }

        BakedModel model = this.itemRenderer.getModel(stack, blockEntity.getLevel(), null, 0);
        double yOffset = model.isGui3d() ? 1.05 : 0.95;

        poseStack.translate(offsetX, yOffset, offsetZ);

        // 使物品平躺
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        
        // 3. 缩放物品
        poseStack.scale(0.4f, 0.4f, 0.4f);

        // 4. 渲染物品
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, combinedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, blockEntity.getLevel(), 0);

        poseStack.popPose();
    }
}
