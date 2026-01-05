package com.github.mczme.arsastra.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JournalTabButton extends AbstractButton {
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/journal_tabs.png");
    
    // 定义每个页签的颜色 (RGB)
    private static final int[] TAB_COLORS = new int[] {
        0xA03030, // Index 0: 典籍 (深红)
        0x804080, // Index 1: 工坊 (魔法紫)
        0x3030A0, // Index 2: 蓝图 (深蓝)
        0xA08030  // Index 3: 星图 (琥珀金)
    };

    private final int index;
    private final OnTabSelected onSelected;
    private boolean isSelected;

    public JournalTabButton(int x, int y, Component message, int index, OnTabSelected onSelected) {
        super(x, y, 41, 20, message); // 默认未选中宽度 41，高度 20
        this.index = index;
        this.onSelected = onSelected;
        this.isSelected = false;
    }

    public void updateState(boolean selected, int baseX) {
        this.isSelected = selected;
        if (selected) {
            this.width = 52;
            this.setX(baseX - 52 + 4); // 选中时伸出更长
        } else {
            this.width = 41;
            this.setX(baseX - 41 + 4);
        }
    }

    @Override
    public void onPress() {
        if (this.onSelected != null) {
            this.onSelected.onSelect(this.index);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        // 1. 获取颜色
        int color = (index >= 0 && index < TAB_COLORS.length) ? TAB_COLORS[index] : 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // 如果未选中，稍微暗一点 (降低亮度)
        if (!isSelected) {
            r *= 0.7f;
            g *= 0.7f;
            b *= 0.7f;
        }

        // 2. 渲染底板 (进行染色)
        guiGraphics.setColor(r, g, b, 1.0f);
        
        if (isSelected) {
            // 选中底板: (0, 20), 52x20
            guiGraphics.blit(TAB_TEXTURE, this.getX(), this.getY(), 0, 20, 52, 20, 64, 64);
        } else {
            // 未选中底板: (0, 0), 41x20
            guiGraphics.blit(TAB_TEXTURE, this.getX(), this.getY(), 0, 0, 41, 20, 64, 64);
        }
        
        // 悬停高亮效果 (可选)
        if (this.isHovered() && !isSelected) {
            // 叠加一层白色半透明
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.2f);
            guiGraphics.blit(TAB_TEXTURE, this.getX(), this.getY(), 0, 0, 41, 20, 64, 64);
        }

        // 3. 渲染图标 (重置颜色，不染色)
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        int iconU = this.index * 16;
        int iconV = 40;
        
        // 计算图标位置：居中后 + 2 像素向右
        int iconX = this.getX() + (this.width - 16) / 2 + 2; 
        int iconY = this.getY() + (this.height - 16) / 2;
        
        guiGraphics.blit(TAB_TEXTURE, iconX, iconY, iconU, iconV, 16, 16, 64, 64);
        
        // 移除了之前的 fill 蒙版，解决了深色正方形问题
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @FunctionalInterface
    public interface OnTabSelected {
        void onSelect(int index);
    }
}
