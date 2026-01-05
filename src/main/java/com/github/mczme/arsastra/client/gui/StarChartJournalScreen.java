package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.client.gui.widget.JournalTabButton;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.menu.StarChartJournalMenu;
import com.github.mczme.arsastra.network.payload.DeductionResultPayload;
import com.github.mczme.arsastra.network.payload.RequestDeductionPayload;
import com.github.mczme.arsastra.registry.AAAttachments;
import com.github.mczme.arsastra.registry.AARegistries;
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
    private static final int TAB_WIDTH = 30;
    
    private int activeTab = 1; // 默认为推演工坊 (页签 1)
    private StarChartWidget starChartWidget;
    private PlayerKnowledge knowledge;
    
    private final List<JournalTabButton> tabButtons = new ArrayList<>();
    
    // 典籍分页相关
    private final List<Element> allElements = new ArrayList<>();
    private Element selectedElement;
    
    // 推演工坊相关
    private final List<ItemStack> inputSequence = new ArrayList<>(8);
    private float lastPredictedStability = 1.0f;

    public StarChartJournalScreen(StarChartJournalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BOOK_WIDTH;
        this.imageHeight = BOOK_HEIGHT;
        
        // 缓存所有要素
        AARegistries.ELEMENT_REGISTRY.forEach(allElements::add);
        
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

        // 初始化页签按钮 (高度 20，间距设为 24)
        createTabButton(0, x, y + 20, "compendium");
        createTabButton(1, x, y + 44, "workshop");
        createTabButton(2, x, y + 68, "blueprints");
        createTabButton(3, x, y + 92, "atlas");

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
        
        // 更新所有按钮状态
        for (JournalTabButton btn : tabButtons) {
            // 我们需要在这里传递 baseX 吗？JournalTabButton 内部已经存了 index 吗？
            // 为了简化，我们在 Button 里不存 index，而是遍历时判断
            // 但我们在 init 里是直接 new JournalTabButton 的。
            // 让我们使用 updateState 方法
            // 这里的 btn 是我们刚刚添加进去的，我们并没有简单的方法获取它的 index 除非我们在 Button 里存了 (我们在上一步确实存了)
            // 但是 updateState 需要 baseX 来重置位置
             // 实际上 Button 知道自己的 index 吗？不知道，它只知道 onSelected 回调。
             // 噢，我在 JournalTabButton 里加了 index 字段。
             
             // 等等，我没有暴露 index 的 getter。这使得我们无法简单判断哪个是哪个。
             // 不过我们在 init 里是按顺序添加的。tabButtons.get(i) 对应 index i。
        }
        
        for (int i = 0; i < tabButtons.size(); i++) {
             tabButtons.get(i).updateState(i == tabIndex, x);
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

        // 2. 根据页签绘制特定内容
        if (activeTab == 0) {
            renderCompendiumLayout(guiGraphics, x, y, mouseX, mouseY);
        } else if (activeTab == 1) {
            renderWorkshopLayout(guiGraphics, x, y);
        }
    }

    private void renderCompendiumLayout(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.ars_astra.journal.compendium.elements"), x + 15, y + 15, 0x404040, false);
        
        int elementY = y + 30;
        for (Element element : allElements) {
            ResourceLocation id = AARegistries.ELEMENT_REGISTRY.getKey(element);
            boolean unlocked = knowledge != null && knowledge.hasUnlockedElement(id);
            
            Component name = unlocked ? Component.translatable(element.getDescriptionId()) : Component.literal("???");
            int color = (selectedElement == element) ? 0xFF000000 : 0x77404040;
            
            guiGraphics.drawString(this.font, name, x + 20, elementY, color, false);
            
            if (mouseX >= x + 20 && mouseX <= x + 140 && mouseY >= elementY && mouseY <= elementY + 9) {
                if (unlocked) {
                    guiGraphics.fill(x + 18, elementY - 1, x + 20, elementY + 8, 0xFF5D4037);
                }
            }
            elementY += 12;
        }

        if (selectedElement != null) {
            guiGraphics.drawString(this.font, Component.translatable(selectedElement.getDescriptionId()), x + BOOK_WIDTH / 2 + 10, y + 15, 0x000000, false);
        } else {
            Component prompt = Component.translatable("gui.ars_astra.journal.compendium.select_element");
            guiGraphics.drawString(this.font, prompt, x + BOOK_WIDTH / 2 + 10, y + BOOK_HEIGHT / 2, 0x88404040, false);
        }
    }

    private void renderWorkshopLayout(GuiGraphics guiGraphics, int x, int y) {
        int slotStartY = y + BOOK_HEIGHT - 35;
        guiGraphics.drawString(this.font, Component.translatable("gui.ars_astra.journal.workshop.sequence"), x + 15, slotStartY - 12, 0x404040, false);
        
        for (int i = 0; i < 8; i++) {
            int slotX = x + 15 + i * 20;
            // 绘制槽位背景高亮（如果 png 里没有预画）
            guiGraphics.fill(slotX, slotStartY, slotX + 18, slotStartY + 18, 0x22000000);
            
            ItemStack stack = inputSequence.get(i);
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, slotX + 1, slotStartY + 1);
            }
        }
        
        // 显示稳定性
        String stabilityText = String.format("Stability: %.0f%%", lastPredictedStability * 100);
        int stabilityColor = (lastPredictedStability > 0.8f) ? 0xFF00AA00 : (lastPredictedStability > 0.5f ? 0xFF666600 : 0xFFAA0000);
        guiGraphics.drawString(this.font, stabilityText, x + BOOK_WIDTH - this.font.width(stabilityText) - 15, slotStartY - 12, stabilityColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - BOOK_WIDTH) / 2;
        int y = (this.height - BOOK_HEIGHT) / 2;

        if (activeTab == 0) {
            int elementY = y + 25;
            for (Element element : allElements) {
                ResourceLocation id = AARegistries.ELEMENT_REGISTRY.getKey(element);
                if (knowledge != null && knowledge.hasUnlockedElement(id)) {
                    if (mouseX >= x + 15 && mouseX <= x + 130 && mouseY >= elementY && mouseY <= elementY + 9) {
                        this.selectedElement = element;
                        return true;
                    }
                }
                elementY += 12;
            }
        } else if (activeTab == 1) {
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
