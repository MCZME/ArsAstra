package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    private StarChart starChart;
    private PlayerKnowledge knowledge;
    
    // 推演预测
    private List<Vector2f> predictionPath;
    private float predictedStability;
    
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

    public void setKnowledge(PlayerKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public void setPrediction(List<Vector2f> path, float stability) {
        this.predictionPath = path;
        this.predictedStability = stability;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 剪裁测试
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        
        // 绘制背景
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF101015);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX() + getWidth() / 2.0f, getY() + getHeight() / 2.0f, 0);
        guiGraphics.pose().translate(offsetX, offsetY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        renderAxes(guiGraphics);

        if (starChart != null) {
            renderStarChart(guiGraphics);
        }

        // 绘制推演路径
        if (predictionPath != null && !predictionPath.isEmpty()) {
            renderPredictionPath(guiGraphics);
        }

        guiGraphics.pose().popPose();
        
        // 绘制边框
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF5D4037);

        guiGraphics.disableScissor();
    }

    private void renderAxes(GuiGraphics guiGraphics) {
        int axisColor = 0x33FFFFFF;
        int length = 1000;
        guiGraphics.fill(-length, 0, length, 1, axisColor);
        guiGraphics.fill(0, -length, 1, length, axisColor);
    }

    private void renderStarChart(GuiGraphics guiGraphics) {
        // TODO
    }

    private void renderPredictionPath(GuiGraphics guiGraphics) {
        // 使用简单的点或线段绘制路径
        int color = 0xFFAAAAAA; // 基础灰色
        // 根据稳定度调整颜色
        if (predictedStability > 0.8f) color = 0xFF55FFFF; // 青色
        else if (predictedStability > 0.5f) color = 0xFFFFFF55; // 黄色
        else color = 0xFFFF5555; // 红色
        
        for (int i = 0; i < predictionPath.size() - 1; i++) {
            Vector2f p1 = predictionPath.get(i);
            Vector2f p2 = predictionPath.get(i + 1);
            // 简单的点绘制 (如果距离近)
            guiGraphics.fill((int)p1.x, (int)p1.y, (int)p1.x + 1, (int)p1.y + 1, color);
        }
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
    }
}
