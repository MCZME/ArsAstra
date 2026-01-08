package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.widget.StarChartFullScreen;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AtlasTab implements JournalTab {
    private StarChartJournalScreen screen;
    private StarChartWidget starChartWidget;
    private ToolbarWidget toolbar;
    
    private List<ResourceLocation> visitedCharts = new ArrayList<>();
    private int currentChartIndex = 0;

    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        PlayerKnowledge knowledge = screen.getPlayerKnowledge();
        if (knowledge != null) {
            this.visitedCharts = new ArrayList<>(knowledge.getVisitedStarCharts());
        }

        // 1. 初始化星图展示组件
        this.starChartWidget = new StarChartWidget(x + 15, y + 10, width - 30, height - 20, Component.empty());
        this.starChartWidget.setKnowledge(knowledge);
        this.starChartWidget.visible = false;
        screen.addTabWidget(this.starChartWidget);

        // 2. 初始化顶部工具栏
        this.toolbar = new ToolbarWidget(x + 15, y - 13, 200, 22);
        
        // 上一个星图
        this.toolbar.addChild(new ToolbarTabButton(0, 0, 20, 22, Component.empty(), 4, 0xFFE0E0E0, this::prevChart));
        // 下一个星图
        this.toolbar.addChild(new ToolbarTabButton(0, 0, 20, 22, Component.empty(), 5, 0xFFE0E0E0, this::nextChart));
        // 全屏按钮
        this.toolbar.addChild(new ToolbarTabButton(0, 0, 20, 22, Component.empty(), 6, 0xFF555555, this::openFullScreen));
        
        this.toolbar.visible = false;
        screen.addTabWidget(this.toolbar);

        updateChartDisplay();
    }

    private void prevChart() {
        if (visitedCharts.isEmpty()) return;
        currentChartIndex = (currentChartIndex - 1 + visitedCharts.size()) % visitedCharts.size();
        updateChartDisplay();
    }

    private void nextChart() {
        if (visitedCharts.isEmpty()) return;
        currentChartIndex = (currentChartIndex + 1) % visitedCharts.size();
        updateChartDisplay();
    }

    private void updateChartDisplay() {
        if (visitedCharts.isEmpty()) return;
        ResourceLocation id = visitedCharts.get(currentChartIndex);
        StarChartManager.getInstance().getStarChart(id).ifPresent(starChartWidget::setStarChart);
    }

    private void openFullScreen() {
        if (starChartWidget != null && visitedCharts.size() > currentChartIndex) {
            ResourceLocation id = visitedCharts.get(currentChartIndex);
            Minecraft.getInstance().setScreen(new StarChartFullScreen(id, screen));
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visitedCharts.isEmpty()) {
            ResourceLocation id = visitedCharts.get(currentChartIndex);
            String name = id.toString();
            int textWidth = Minecraft.getInstance().font.width(name);
            int widgetRight = starChartWidget.getX() + starChartWidget.getWidth();
            int widgetTop = starChartWidget.getY();
            
            // 绘制背景 (右上角，留出 5px 边距)
            int bgX = widgetRight - textWidth - 10;
            int bgY = widgetTop + 5;
            guiGraphics.fill(bgX, bgY, bgX + textWidth + 6, bgY + 12, 0x80000000);
            
            // 绘制文字
            guiGraphics.drawString(Minecraft.getInstance().font, name, bgX + 3, bgY + 2, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.atlas.empty"), screen.width / 2, screen.height / 2, 0x404040);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (starChartWidget != null) starChartWidget.visible = visible;
        if (toolbar != null) toolbar.visible = visible;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY, button)) return true;
        if (starChartWidget != null && starChartWidget.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (starChartWidget != null && starChartWidget.mouseReleased(mouseX, mouseY, button)) handled = true;
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // StarChartWidget 需要处理拖拽事件
        if (starChartWidget != null && starChartWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (toolbar != null && toolbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (starChartWidget != null && starChartWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }
}
