package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ToolbarSearchBox extends EditBox {
    
    public ToolbarSearchBox(Font font, int width, int height, Component message) {
        super(font, 0, 0, width, height, message); // 位置由 ToolbarWidget 决定
        this.setBordered(false); 
        this.setTextColor(0x404040);
        this.setMaxLength(32);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        
        // 背景框：浅色底，棕色下划线
        if (this.isFocused()) {
             guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFF4A3525); // 深棕色高亮
        } else {
             guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFFA08060); // 浅棕色
        }
        
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        
        if (this.getValue().isEmpty() && !this.isFocused()) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.journal.compendium.search"), x + 1, y + (h - 8) / 2, 0x999999, false);
        }
    }
}
