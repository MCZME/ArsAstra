package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.core.starchart.StarChartManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StarChartFullScreen extends Screen {
    private final ResourceLocation chartId;
    private final Screen parent;
    private StarChartWidget starChartWidget;

    public StarChartFullScreen(ResourceLocation chartId, Screen parent) {
        super(Component.translatable("gui.ars_astra.atlas.fullscreen"));
        this.chartId = chartId;
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 1. 全屏星图画布
        this.starChartWidget = new StarChartWidget(0, 0, this.width, this.height, Component.empty());
        StarChartManager.getInstance().getStarChart(chartId).ifPresent(starChartWidget::setStarChart);
        this.addRenderableWidget(this.starChartWidget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 显示星图 ID
        guiGraphics.drawString(this.font, chartId.toString(), 10, 10, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
