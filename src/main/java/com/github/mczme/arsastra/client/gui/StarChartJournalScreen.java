package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.client.gui.widget.JournalTabButton;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import com.github.mczme.arsastra.client.gui.widget.compendium.CompendiumWidget;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.menu.StarChartJournalMenu;
import com.github.mczme.arsastra.network.payload.DeductionResultPayload;
import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class StarChartJournalScreen extends AbstractContainerScreen<StarChartJournalMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/star_chart_journal_gui.png");
    
    // UI 尺寸常量
    private static final int BOOK_WIDTH = 300;
    private static final int BOOK_HEIGHT = 176;
    
    private int activeTab = 1; // 默认为推演工坊 (页签 1)
    private StarChartWidget starChartWidget;
    private CompendiumWidget compendiumWidget;
    private PlayerKnowledge knowledge;
    
    private final List<JournalTabButton> tabButtons = new ArrayList<>();
    
    // 推演工坊相关
    private final List<ItemStack> inputSequence = new ArrayList<>(8);
    private float lastPredictedStability = 1.0f;

    public StarChartJournalScreen(StarChartJournalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BOOK_WIDTH;
        this.imageHeight = BOOK_HEIGHT;
        
        // 初始化序列
        for (int i = 0; i < 8; i++) {
            inputSequence.add(ItemStack.EMPTY);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.knowledge = Minecraft.getInstance().player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        this.tabButtons.clear();
        
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        // 初始化页签按钮
        createTabButton(0, x, y + 20, "compendium");
        createTabButton(1, x, y + 44, "workshop");
        createTabButton(2, x, y + 68, "blueprints");
        createTabButton(3, x, y + 92, "atlas");

        // 初始化典籍组件
        this.compendiumWidget = new CompendiumWidget(x, y, BOOK_WIDTH, BOOK_HEIGHT, this.knowledge);
        this.addRenderableWidget(this.compendiumWidget);

        // 初始化星图组件
        this.starChartWidget = new StarChartWidget(x + 10, y + 10, BOOK_WIDTH - 20, BOOK_HEIGHT - 50, Component.empty());
        this.starChartWidget.setKnowledge(this.knowledge);
        this.addRenderableWidget(this.starChartWidget);
        
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

        if (this.compendiumWidget != null) {
            this.compendiumWidget.visible = (tabIndex == 0);
        }

        if (this.starChartWidget != null) {
            this.starChartWidget.visible = (tabIndex == 1 || tabIndex == 3);
            if (tabIndex == 1) {
                this.starChartWidget.setWidth(BOOK_WIDTH - 20);
                this.starChartWidget.setHeight(BOOK_HEIGHT - 50);
            } else if (tabIndex == 3) {
                this.starChartWidget.setWidth(BOOK_WIDTH - 20);
                this.starChartWidget.setHeight(BOOK_HEIGHT - 20);
            }
        }
    }

    public void handleDeductionResult(DeductionResultPayload payload) {
        if (this.starChartWidget != null) {
            this.starChartWidget.setPrediction(payload.points(), payload.stability());
            this.lastPredictedStability = payload.stability();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;
        
        // 1. 绘制书本背景贴图
        guiGraphics.blit(TEXTURE, x, y, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);

        // 2. 根据页签绘制特定内容 (工坊仍暂存此处，直到被重构为 Widget)
        if (activeTab == 1) {
            renderWorkshopLayout(guiGraphics, x, y);
        }
    }

    private void renderWorkshopLayout(GuiGraphics guiGraphics, int x, int y) {
        int slotStartY = y + BOOK_HEIGHT - 35;
        guiGraphics.drawString(this.font, Component.translatable("gui.ars_astra.journal.workshop.sequence"), x + 15, slotStartY - 12, 0x404040, false);
        
        for (int i = 0; i < 8; i++) {
            int slotX = x + 15 + i * 20;
            guiGraphics.fill(slotX, slotStartY, slotX + 18, slotStartY + 18, 0x22000000);
            
            ItemStack stack = inputSequence.get(i);
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, slotX + 1, slotStartY + 1);
            }
        }
        
        String stabilityText = String.format("Stability: %.0f%%", lastPredictedStability * 100);
        int stabilityColor = (lastPredictedStability > 0.8f) ? 0xFF00AA00 : (lastPredictedStability > 0.5f ? 0xFF666600 : 0xFFAA0000);
        guiGraphics.drawString(this.font, stabilityText, x + BOOK_WIDTH - this.font.width(stabilityText) - 15, slotStartY - 12, stabilityColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        if (activeTab == 1) {
            int slotStartY = y + BOOK_HEIGHT - 35;
            for (int i = 0; i < 8; i++) {
                int slotX = x + 10 + i * 20;
                if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotStartY && mouseY <= slotStartY + 18) {
                    handleGhostSlotClick(i, button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleGhostSlotClick(int index, int button) {
        ItemStack carried = menu.getCarried();
        boolean changed = false;
        if (button == 0) { // 左键
            if (!carried.isEmpty()) {
                ItemStack toPut = carried.copy();
                toPut.setCount(1);
                inputSequence.set(index, toPut);
                changed = true;
            } else {
                if (!inputSequence.get(index).isEmpty()) {
                    inputSequence.set(index, ItemStack.EMPTY);
                    changed = true;
                }
            }
        } else if (button == 1) { // 右键
            if (!inputSequence.get(index).isEmpty()) {
                inputSequence.set(index, ItemStack.EMPTY);
                changed = true;
            }
        }
        
        if (changed) {
            PacketDistributor.sendToServer(new RequestDeductionPayload(new ArrayList<>(inputSequence)));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
