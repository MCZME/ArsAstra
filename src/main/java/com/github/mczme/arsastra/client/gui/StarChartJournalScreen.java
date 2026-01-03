package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import com.github.mczme.arsastra.menu.StarChartJournalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StarChartJournalScreen extends AbstractContainerScreen<StarChartJournalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/star_chart_journal.png");
    private int activeTab = 1; // 默认为推演工坊 (页签 1)
    private StarChartWidget starChartWidget;

    public StarChartJournalScreen(StarChartJournalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 初始化页签按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.journal.tab.compendium"), b -> switchTab(0))
                .bounds(x - 30, y + 20, 30, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.journal.tab.workshop"), b -> switchTab(1))
                .bounds(x - 30, y + 45, 30, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.journal.tab.blueprints"), b -> switchTab(2))
                .bounds(x - 30, y + 70, 30, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.journal.tab.atlas"), b -> switchTab(3))
                .bounds(x - 30, y + 95, 30, 20).build());

        // 初始化星图组件 (位置在右侧区域)
        // 调整位置以适应新的布局
        this.starChartWidget = new StarChartWidget(x + 85, y + 25, 160, 130, Component.empty());
        this.addRenderableWidget(this.starChartWidget);
        
        // 初始刷新
        switchTab(activeTab);
    }

    private void switchTab(int tabIndex) {
        this.activeTab = tabIndex;
        // 仅在工坊(1)和星图(3)页签显示星图组件
        if (this.starChartWidget != null) {
            this.starChartWidget.visible = (tabIndex == 1 || tabIndex == 3);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // 根据页签绘制特定的背景元素
        if (activeTab == 1) { // 推演工坊
            renderWorkshopLayout(guiGraphics, x, y);
        }
    }

    private void renderWorkshopLayout(GuiGraphics guiGraphics, int x, int y) {
        // 1. 绘制左侧“要素典籍”列表背景
        guiGraphics.fill(x + 10, y + 25, x + 80, y + 155, 0xFFC6C6C6); // 浅灰色占位
        guiGraphics.fill(x + 11, y + 26, x + 79, y + 154, 0xFF8B8B8B); // 深灰色边框内填充
        
        // 2. 绘制下方虚拟投料槽
        int slotStartX = x + 85;
        int slotStartY = y + 160;
        for (int i = 0; i < 8; i++) {
            int slotX = slotStartX + i * 20;
            // 绘制槽位框
            guiGraphics.fill(slotX, slotStartY, slotX + 18, slotStartY + 18, 0xFF8B8B8B);
            guiGraphics.fill(slotX + 1, slotStartY + 1, slotX + 17, slotStartY + 17, 0xFF373737);
        }
        
        // 3. 绘制标题文本（只是简单的标签）
        guiGraphics.drawString(this.font, Component.literal("Materials"), x + 10, y + 15, 0x404040, false);
        guiGraphics.drawString(this.font, Component.literal("Input Sequence"), x + 85, y + 150, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // 绘制当前页签名称
        Component tabName = switch (activeTab) {
            case 0 -> Component.translatable("gui.ars_astra.journal.tab.compendium");
            case 1 -> Component.translatable("gui.ars_astra.journal.tab.workshop");
            case 2 -> Component.translatable("gui.ars_astra.journal.tab.blueprints");
            case 3 -> Component.translatable("gui.ars_astra.journal.tab.atlas");
            default -> Component.empty();
        };
        guiGraphics.drawString(this.font, tabName, this.imageWidth / 2 - this.font.width(tabName) / 2, 10, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
