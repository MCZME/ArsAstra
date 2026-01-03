package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.core.starchart.StarChart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class StarChartWidget extends AbstractWidget {
    private StarChart starChart;
    
    // 视口状态
    private float scale = 1.0f;
    private float offsetX = 0;
    private float offsetY = 0;
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setStarChart(StarChart starChart) {
        this.starChart = starChart;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 剪裁测试，确保内容不超出组件边界
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        
        // 绘制背景
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF101015); // 深邃空间背景

        // 推入矩阵以进行变换
        guiGraphics.pose().pushPose();
        // 平移到组件中心
        guiGraphics.pose().translate(getX() + getWidth() / 2.0f, getY() + getHeight() / 2.0f, 0);
        // 应用平移和缩放
        guiGraphics.pose().translate(offsetX, offsetY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // 绘制坐标轴
        renderAxes(guiGraphics);

        // 绘制星图元素 (环境、效果星域)
        if (starChart != null) {
            renderStarChart(guiGraphics);
        }

        // TODO: 当合并推演系统分支后，在此处绘制推演路径 (Deduction Path)

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void renderAxes(GuiGraphics guiGraphics) {
        int axisColor = 0x55FFFFFF;
        int length = 1000;
        // X 轴
        guiGraphics.fill(-length, -1, length, 1, axisColor);
        // Y 轴
        guiGraphics.fill(-1, -length, 1, length, axisColor);
    }

    private void renderStarChart(GuiGraphics guiGraphics) {
        // TODO: 遍历并绘制 starChart 中的环境和效果区域
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && button == 0) {
            this.isDragging = true;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
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
            this.offsetX += (float) dragX;
            this.offsetY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            float zoomFactor = 1.1f;
            if (scrollY > 0) {
                this.scale *= zoomFactor;
            } else {
                this.scale /= zoomFactor;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 可访问性支持
    }
}
