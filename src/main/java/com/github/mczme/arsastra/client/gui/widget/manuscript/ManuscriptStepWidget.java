package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class ManuscriptStepWidget {
    private final AlchemyInput input;
    private final int index;
    
    // 布局状态 (由父级每帧更新)
    private float x, y;
    private float scale;
    private boolean visible;

    // 动画状态
    private float hoverProgress = 0f;
    private static final float ANIMATION_SPEED = 0.4f;

    public ManuscriptStepWidget(AlchemyInput input, int index) {
        this.input = input;
        this.index = index;
    }

    public void updateLayout(float x, float y, float scale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.visible = true;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        // 1. 更新悬停动画
        float baseSize = 16f;
        float currentSize = baseSize * scale;
        float left = x - currentSize / 2;
        float top = y - currentSize / 2;
        
        boolean isHovered = mouseX >= left && mouseX <= left + currentSize &&
                            mouseY >= top && mouseY <= top + currentSize;

        float targetProgress = isHovered ? 1.0f : 0.0f;
        this.hoverProgress = Mth.lerp(partialTick * ANIMATION_SPEED, this.hoverProgress, targetProgress);

        // 2. 渲染基础边框 (LOD)
        int borderColor = 0xFF4A3B2A;
        int backgroundColor = 0x204A3B2A; 

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // 绘制厚边框 (2px)
        guiGraphics.fill(-6, -6, 6, 6, borderColor);
        guiGraphics.fill(-4, -4, 4, 4, backgroundColor);
        
        guiGraphics.pose().popPose(); 

        // 3. 渲染内容 (LOD)
        if (scale > 0.5f) {
            int borderHalf = 6;
            int sx1 = (int)(x - borderHalf * scale);
            int sy1 = (int)(y - borderHalf * scale);
            int sx2 = (int)(x + borderHalf * scale);
            int sy2 = (int)(y + borderHalf * scale);

            guiGraphics.enableScissor(sx1, sy1, sx2, sy2);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            
            ItemStack stack = input.stack();
            guiGraphics.pose().translate(-8, -8, 10); 
            guiGraphics.renderItem(stack, 0, 0);
            
            guiGraphics.pose().popPose();
            
            guiGraphics.disableScissor();
        } else {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.fill(-2, -2, 3, 3, borderColor);
            guiGraphics.pose().popPose();
        }

        // 4. 渲染展开详情 (Overlay)
        if (hoverProgress > 0.01f) {
            renderExpandedDetails(guiGraphics, font, mouseX, mouseY);
        }
    }

    private void renderExpandedDetails(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        float expansionScale = hoverProgress;
        
        int panelWidth = 100;
        int panelHeight = 40;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300); 
        
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(expansionScale, expansionScale, 1.0f);
        
        int bgColor = 0xF0100010; 
        
        int px = -panelWidth / 2;
        int py = -panelHeight - 15; 
        
        guiGraphics.fillGradient(px, py, px + panelWidth, py + panelHeight, bgColor, bgColor);
        guiGraphics.renderOutline(px, py, panelWidth, panelHeight, 0xFFFFFFFF); 

        if (expansionScale > 0.8f) {
            Component title = input.stack().getHoverName();
            int titleW = font.width(title);
            guiGraphics.drawString(font, title, px + (panelWidth - titleW) / 2, py + 5, 0xFFFFFFFF, false);

            // 使用本地化组件
            Component stepComp = Component.translatable("gui.ars_astra.manuscript.step", index + 1);
            guiGraphics.drawString(font, stepComp, px + 5, py + 18, 0xFFAAAAAA, false);

            Component rotComp = Component.translatable("gui.ars_astra.manuscript.rotation", String.format("%.1f", input.rotation()));
            int rotColor = Math.abs(input.rotation()) > 0.001f ? 0xFFFF5555 : 0xFFAAAAAA;
            guiGraphics.drawString(font, rotComp, px + 5, py + 28, rotColor, false);
        }

        guiGraphics.pose().popPose();
    }
}
