package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ToolbarTabButton extends AbstractButton {
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/journal_tabs.png");
    
    private final int color;
    private final int iconIndex;
    private final Runnable onPress;
    private boolean isSelected;

    public ToolbarTabButton(int x, int y, int width, int height, Component message, int iconIndex, int color, Runnable onPress) {
        super(x, y, width, height, message);
        this.iconIndex = iconIndex;
        this.color = color;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    @Override
    public void onPress() {
        if (onPress != null) onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // 1. 计算颜色层级
        // 基础颜色 (Base)
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // 状态调整：未选中时整体变暗
        if (!isSelected) {
            r = (int)(r * 0.7f);
            g = (int)(g * 0.7f);
            b = (int)(b * 0.7f);
        }
        
        // 悬停高亮：整体提亮
        if (isHovered()) {
            r = Math.min(255, (int)(r * 1.2f));
            g = Math.min(255, (int)(g * 1.2f));
            b = Math.min(255, (int)(b * 1.2f));
        }

        int mainColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        
        // 边框色 (Deepest): 0.4x
        int borderR = (int)(r * 0.4f);
        int borderG = (int)(g * 0.4f);
        int borderB = (int)(b * 0.4f);
        int borderColor = 0xFF000000 | (borderR << 16) | (borderG << 8) | borderB;

        // 装饰条色 (Mid-Dark): 0.7x (即比主色深，但比边框浅)
        int accentR = (int)(r * 0.7f);
        int accentG = (int)(g * 0.7f);
        int accentB = (int)(b * 0.7f);
        int accentColor = 0xFF000000 | (accentR << 16) | (accentG << 8) | accentB;

        // 2. 绘制图层
        // Layer 1: 边框 (填充整个区域，之后中间会被覆盖)
        guiGraphics.fill(x, y, x + w, y + h, borderColor);

        // Layer 2: 主背景 (向内缩进 2px)
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, mainColor);

        // Layer 3: 底部装饰条 (在内轮廓内部的底部，高 4px)
        guiGraphics.fill(x + 2, y + h - 6, x + w - 2, y + h - 2, accentColor);

        // 3. 渲染图标
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int iconU = iconIndex * 16;
        int iconV = 40; 
        
        // 计算图标位置居中
        guiGraphics.blit(TAB_TEXTURE, x + (w - 16) / 2, y + (h - 16) / 2, iconU, iconV, 16, 16, 64, 64);
        
        RenderSystem.disableBlend();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
