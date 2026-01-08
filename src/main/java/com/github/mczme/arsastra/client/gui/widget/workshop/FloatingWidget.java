package com.github.mczme.arsastra.client.gui.widget.workshop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class FloatingWidget extends AbstractWidget {
    protected boolean isDragging = false;
    protected double dragAnchorX, dragAnchorY;
    private final int titleHeight = 12;

    public FloatingWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render Background
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xCC101010);
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF404040);
        
        // Render Title Bar
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + titleHeight, 0xFF303030);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + 4, getY() + 2, 0xFFFFFF, false);
    }
    
    protected boolean isMouseOverTitle(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + titleHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && button == 0) {
            if (this.isMouseOver(mouseX, mouseY)) {
                if (isMouseOverTitle(mouseX, mouseY)) {
                    this.isDragging = true;
                    // 记录鼠标相对于组件左上角的偏移量（锚点）
                    this.dragAnchorX = mouseX - this.getX();
                    this.dragAnchorY = mouseY - this.getY();
                }
                return true; // 即使不是标题栏，只要在范围内就消耗点击，防止穿透
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            // 使用当前鼠标位置减去锚点，得到新的精确位置
            this.setX((int) (mouseX - this.dragAnchorX));
            this.setY((int) (mouseY - this.dragAnchorY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
