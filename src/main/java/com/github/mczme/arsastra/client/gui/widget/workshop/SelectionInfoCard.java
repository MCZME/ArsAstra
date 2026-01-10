package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class SelectionInfoCard extends AbstractWidget {
    private final WorkshopSession session;
    private float animationProgress = 0f;
    private static final int MAX_HEIGHT = 50;
    private static final int CARD_WIDTH = 120;

    // y 坐标作为卡片的底部锚点
    public SelectionInfoCard(int x, int bottomY, WorkshopSession session) {
        super(x, bottomY, CARD_WIDTH, 0, Component.empty());
        this.session = session;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AlchemyInput input = session.getSelectedInput();
        boolean shouldShow = (input != null);

        // 简单的动画平滑 (每帧逼近目标值)
        float target = shouldShow ? 1.0f : 0.0f;
        float speed = 0.2f; // 动画速度
        
        // 考虑到帧率波动，这里做一个简单的线性插值
        if (Math.abs(target - animationProgress) > 0.001f) {
            animationProgress = animationProgress + (target - animationProgress) * speed;
        } else {
            animationProgress = target;
        }
        
        if (this.animationProgress < 0.01f) return;

        int currentHeight = (int) (MAX_HEIGHT * this.animationProgress);
        // 计算绘制的起始 Y (向上展开)
        int drawY = getY() - currentHeight; 
        
        // 绘制卡片背景
        guiGraphics.fill(getX(), drawY, getX() + width, getY(), Palette.PARCHMENT_BG);
        guiGraphics.renderOutline(getX(), drawY, width, currentHeight, Palette.INK);
        
        // 绘制装饰性顶部横条
        if (currentHeight > 4) {
            guiGraphics.fill(getX(), drawY, getX() + width, drawY + 4, Palette.INK);
        }

        // 仅当展开程度足够时显示内容，并进行淡入处理 (这里通过透明度或裁剪简化)
        if (this.animationProgress > 0.5f && input != null) {
            // 启用剪裁以防止文字溢出边界
            guiGraphics.enableScissor(getX(), drawY, getX() + width, getY());
            
            int padding = 8;
            int contentY = drawY + 10;
            
            // 物品图标
            guiGraphics.renderFakeItem(input.stack(), getX() + padding, contentY);
            
            // 物品名称
            int textX = getX() + padding + 20;
            guiGraphics.drawString(Minecraft.getInstance().font, input.stack().getHoverName(), textX, contentY + 4, Palette.INK, false);
            
            contentY += 20;
            
            // 分割线
            guiGraphics.fill(getX() + padding, contentY, getX() + width - padding, contentY + 1, Palette.INK_LIGHT);
            contentY += 6;

            // 旋转信息
            float degrees = (float) Math.toDegrees(input.rotation());
            Component rotComp = Component.translatable("gui.ars_astra.workshop.rotation", String.format("%.1f", degrees));
            guiGraphics.drawString(Minecraft.getInstance().font, rotComp, getX() + padding, contentY, Palette.INK_LIGHT, false);
            
            guiGraphics.disableScissor();
        }
        
        // 更新 widget 的实际高度以响应点击 (虽然目前卡片不响应点击，但保持状态一致是好的)
        this.height = currentHeight;
        // 注意：getY() 是底部，所以 Widget 的逻辑 Y 应该是 drawY。
        // 但 AbstractWidget 的 setY 会改变 getY() 的返回值。
        // 我们保持构造函数传入的 Y 为底部锚点不动，只在渲染时计算 drawY。
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}