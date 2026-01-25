package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SelectionInfoCard extends AbstractWidget {
    private final WorkshopSession session;
    private float animationProgress = 0f;
    private static final int MAX_HEIGHT = 50;
    private static final int CARD_WIDTH = 120;
    private final int bottomY;

    // y 坐标作为卡片的底部锚点
    public SelectionInfoCard(int x, int bottomY, WorkshopSession session) {
        super(x, bottomY, CARD_WIDTH, 0, Component.empty());
        this.bottomY = bottomY;
        this.session = session;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AlchemyInput input = session.getSelectedInput();
        boolean shouldShow = (input != null);

        // 简单的动画平滑
        float target = shouldShow ? 1.0f : 0.0f;
        float speed = 0.2f;
        
        if (Math.abs(target - animationProgress) > 0.001f) {
            animationProgress = animationProgress + (target - animationProgress) * speed;
        } else {
            animationProgress = target;
        }
        
        if (this.animationProgress < 0.01f) {
            this.height = 0;
            return;
        }

        guiGraphics.pose().pushPose();
        // 提升 Z 轴层级，盖住下方的物品
        guiGraphics.pose().translate(0, 0, 180);

        int currentHeight = (int) (MAX_HEIGHT * this.animationProgress);
        // 计算绘制的起始 Y (向上展开)
        int drawY = this.bottomY - currentHeight; 
        
        // 关键修复：SelectionInfoCard 不应有逻辑高度来遮挡下方组件
        this.height = 0;

        // 绘制卡片背景
        guiGraphics.fill(getX(), drawY, getX() + width, this.bottomY, Palette.PARCHMENT_BG);
        guiGraphics.renderOutline(getX(), drawY, width, currentHeight, Palette.INK);
        
        // 绘制装饰性顶部横条
        if (currentHeight > 4) {
            guiGraphics.fill(getX(), drawY, getX() + width, drawY + 4, Palette.INK);
        }

        // 内容显示
        if (this.animationProgress > 0.5f && input != null) {
            guiGraphics.enableScissor(getX(), drawY, getX() + width, this.bottomY);
            
            int padding = 8;
            int contentY = drawY + 10;
            
            guiGraphics.renderFakeItem(input.stack(), getX() + padding, contentY);
            
            int textX = getX() + padding + 20;
            guiGraphics.drawString(Minecraft.getInstance().font, input.stack().getHoverName(), textX, contentY + 4, Palette.INK, false);
            
            contentY += 20;
            guiGraphics.fill(getX() + padding, contentY, getX() + width - padding, contentY + 1, Palette.INK_LIGHT);
            contentY += 6;

            float degrees = (float) input.rotation();
            Component rotComp = Component.translatable("gui.ars_astra.workshop.rotation", String.format("%.1f", degrees));
            guiGraphics.drawString(Minecraft.getInstance().font, rotComp, getX() + padding, contentY, Palette.INK_LIGHT, false);
            
            guiGraphics.disableScissor();
        }
        
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 该组件纯显示，不消耗点击事件，确保下方的序列槽能正常工作
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}