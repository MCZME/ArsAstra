package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.client.gui.tab.AtlasTab;
import com.github.mczme.arsastra.client.gui.tab.CompendiumTab;
import com.github.mczme.arsastra.client.gui.tab.JournalTab;
import com.github.mczme.arsastra.client.gui.tab.WorkshopTab;
import com.github.mczme.arsastra.client.gui.widget.JournalTabButton;
import com.github.mczme.arsastra.network.payload.DeductionResultPayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;

import java.util.ArrayList;
import java.util.List;

public class StarChartJournalScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/star_chart_journal_gui.png");
    
    // UI 尺寸常量
    private static final int BOOK_WIDTH = 300;
    private static final int BOOK_HEIGHT = 176;
    
    private int activeTab = 0; // 默认为 (页签 0) "Compendium"
    
    private final CompendiumTab compendiumTab = new CompendiumTab();
    private final WorkshopTab workshopTab = new WorkshopTab();
    private final AtlasTab atlasTab = new AtlasTab();
    private final List<JournalTabButton> tabButtons = new ArrayList<>();
    
    private PlayerKnowledge playerKnowledge;

    public StarChartJournalScreen() {
        super(Component.translatable("item.ars_astra.star_chart_journal"));
    }
    
    public PlayerKnowledge getPlayerKnowledge() {
        return playerKnowledge;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> void addTabWidget(T widget) {
        this.addRenderableWidget(widget);
    }

    @SuppressWarnings("null")
    @Override
    protected void init() {
        super.init();
        this.playerKnowledge = Minecraft.getInstance().player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        this.tabButtons.clear();
        
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // 初始化页签按钮
        createTabButton(0, x, y + 20, "compendium");
        createTabButton(1, x, y + 44, "workshop");
        createTabButton(2, x, y + 68, "manuscripts");
        createTabButton(3, x, y + 92, "atlas");

        // 初始化各个 Tab
        this.compendiumTab.init(this, x, y, BOOK_WIDTH, BOOK_HEIGHT);
        this.workshopTab.init(this, x, y, BOOK_WIDTH, BOOK_HEIGHT);
        this.atlasTab.init(this, x, y, BOOK_WIDTH, BOOK_HEIGHT);

        switchTab(activeTab);
    }

    private void createTabButton(int index, int baseX, int y, String name) {
        JournalTabButton btn = new JournalTabButton(baseX, y, Component.empty(), index, this::switchTab);
        btn.setTooltip(Tooltip.create(Component.translatable("gui.ars_astra.journal.tab." + name)));
        this.addRenderableWidget(btn);
        this.tabButtons.add(btn);
    }

    private void switchTab(int tabIndex) {
        this.activeTab = tabIndex;
        int x = (this.width - BOOK_WIDTH) / 2;
        
        for (int i = 0; i < tabButtons.size(); i++) {
             tabButtons.get(i).updateState(i == tabIndex, x);
        }

        if (this.compendiumTab != null) {
            this.compendiumTab.setVisible(tabIndex == 0);
        }

        if (this.workshopTab != null) {
            this.workshopTab.setVisible(tabIndex == 1);
        }

        if (this.atlasTab != null) {
            this.atlasTab.setVisible(tabIndex == 3);
        }
    }
    
    private JournalTab getCurrentTab() {
        if (activeTab == 0) return compendiumTab;
        if (activeTab == 1) return workshopTab;
        if (activeTab == 3) return atlasTab;
        return null;
    }

    public void handleDeductionResult(DeductionResultPayload payload) {
        if (this.workshopTab != null && this.workshopTab.getSession() != null) {
            this.workshopTab.getSession().setDeductionResult(payload.result());
        }
    }

    private void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // 1. 绘制书本背景贴图
        guiGraphics.blit(TEXTURE, x, y, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);

        // 2. 根据页签绘制特定内容
        JournalTab tab = getCurrentTab();
        if (tab != null) {
            tab.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (activeTab == 1 && workshopTab != null) {
            workshopTab.renderOverlay(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 事件转发

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        JournalTab tab = getCurrentTab();
        if (tab != null && tab.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);

    }

}