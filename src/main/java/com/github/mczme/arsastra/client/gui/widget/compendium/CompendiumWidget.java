package com.github.mczme.arsastra.client.gui.widget.compendium;

import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarFilterWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.registry.AARegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompendiumWidget extends AbstractWidget {
    private enum DisplayMode { ITEMS, ELEMENTS }

    private final PlayerKnowledge knowledge;
    private final ToolbarWidget toolbar;
    private final ToolbarTabButton modeSwitchButton;
    
    // 数据字段 - 物品模式
    private List<ItemStack> allAnalyzedItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private ItemStack selectedItem = ItemStack.EMPTY;

    // 数据字段 - 要素模式
    private List<Element> allElements = new ArrayList<>();
    private List<Element> filteredElements = new ArrayList<>();
    private Element selectedElement = null;
    
    // 状态字段
    private DisplayMode currentMode = DisplayMode.ITEMS;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 20;
    private String currentSearchQuery = "";
    private int currentFilterIndex = 0;

    public CompendiumWidget(int x, int y, int width, int height, PlayerKnowledge knowledge) {
        super(x, y, width, height, Component.empty());
        this.knowledge = knowledge;
        
        // 初始化工具栏
        this.toolbar = new ToolbarWidget(x + 15, y - 20, 200, 22);
        
        // 1. 模式切换按钮 (初始为 ITEMS 模式, 使用图标索引 2 - 物品)
        this.modeSwitchButton = new ToolbarTabButton(0, 0, 20, 22, Component.empty(), 2, 0x4040A0, this::toggleMode);
        this.toolbar.addChild(this.modeSwitchButton);

        // 2. 搜索组件
        this.toolbar.addChild(new ToolbarSearchWidget(0, 0, (query) -> {
            this.currentSearchQuery = query.toLowerCase();
            this.refreshContent();
        }));

        // 3. 筛选组件
        this.toolbar.addChild(new ToolbarFilterWidget(0, 0, (index) -> {
            this.currentFilterIndex = index;
            this.refreshContent();
        }));

        refreshContent();
    }

    private void toggleMode() {
        if (this.currentMode == DisplayMode.ITEMS) {
            this.currentMode = DisplayMode.ELEMENTS;
            this.modeSwitchButton.setIconIndex(3); // 切换到图标 3 (要素)
            this.modeSwitchButton.setColor(0x804080); // 紫色调
        } else {
            this.currentMode = DisplayMode.ITEMS;
            this.modeSwitchButton.setIconIndex(2); // 切换到图标 2 (物品)
            this.modeSwitchButton.setColor(0x4040A0); // 蓝色调
        }
        this.currentPage = 0;
        refreshContent();
    }

    public void refreshContent() {
        if (currentMode == DisplayMode.ITEMS) {
            refreshItems();
        } else {
            refreshElements();
        }
    }

    private void refreshItems() {
        this.allAnalyzedItems = ElementProfileManager.getInstance().getAllProfiledItems().stream()
                .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)))
                .filter(stack -> knowledge.hasAnalyzed(stack.getItem()))
                .collect(Collectors.toList());
        
        List<ItemStack> afterFilter = this.allAnalyzedItems.stream()
                .filter(stack -> applyItemFilter(stack, currentFilterIndex))
                .collect(Collectors.toList());

        if (!currentSearchQuery.isEmpty()) {
            this.filteredItems = afterFilter.stream()
                    .filter(stack -> stack.getHoverName().getString().toLowerCase().contains(currentSearchQuery))
                    .collect(Collectors.toList());
        } else {
            this.filteredItems = afterFilter;
        }
        
        if (selectedItem.isEmpty() && !filteredItems.isEmpty()) {
            selectedItem = filteredItems.get(0);
        } else if (!filteredItems.contains(selectedItem) && !filteredItems.isEmpty()) {
            selectedItem = filteredItems.get(0);
        }
    }

    private void refreshElements() {
        this.allElements = AARegistries.ELEMENT_REGISTRY.stream().collect(Collectors.toList());
        
        List<Element> afterFilter = this.allElements; 

        if (!currentSearchQuery.isEmpty()) {
            this.filteredElements = afterFilter.stream()
                    .filter(e -> Component.translatable(e.getDescriptionId()).getString().toLowerCase().contains(currentSearchQuery))
                    .collect(Collectors.toList());
        } else {
            this.filteredElements = afterFilter;
        }

        if (selectedElement == null && !filteredElements.isEmpty()) {
            selectedElement = filteredElements.get(0);
        } else if (!filteredElements.contains(selectedElement) && !filteredElements.isEmpty()) {
            selectedElement = filteredElements.get(0);
        }
    }
    
    private boolean applyItemFilter(ItemStack stack, int filterIndex) {
        switch (filterIndex) {
            case 0: return true; 
            case 1: return stack.getItem() instanceof BlockItem; 
            case 2: return ! (stack.getItem() instanceof BlockItem); 
            default: return true;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        toolbar.render(guiGraphics, mouseX, mouseY, partialTick);
        if (currentMode == DisplayMode.ITEMS) {
            renderItemGrid(guiGraphics, mouseX, mouseY);
            renderItemDetails(guiGraphics);
        } else {
            renderElementGrid(guiGraphics, mouseX, mouseY);
            renderElementDetails(guiGraphics);
        }
    }

    private void renderItemGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = this.getX() + 15;
        int startY = this.getY() + 30;
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            ItemStack stack = filteredItems.get(i);
            
            if (selectedItem.getItem() == stack.getItem()) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x40AA8800); 
            } else if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x20000000); 
            }
            guiGraphics.renderFakeItem(stack, slotX, slotY);
        }
        renderPaginator(guiGraphics, filteredItems.size());
    }

    private void renderElementGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = this.getX() + 15;
        int startY = this.getY() + 30;
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredElements.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            Element element = filteredElements.get(i);
            
            if (selectedElement == element) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x40804080); // 紫色高亮
            } else if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x20000000); 
            }
            
            RenderSystem.enableBlend();
            guiGraphics.blit(element.getIcon(), slotX, slotY, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }
        renderPaginator(guiGraphics, filteredElements.size());
    }
    
    private void renderPaginator(GuiGraphics guiGraphics, int totalItems) {
        if (totalItems > ITEMS_PER_PAGE) {
            String pageText = (currentPage + 1) + " / " + (int) Math.ceil(totalItems / (double) ITEMS_PER_PAGE);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, pageText, this.getX() + 70, this.getY() + 145, 0x404040);
            if (currentPage > 0) guiGraphics.drawString(Minecraft.getInstance().font, "<", this.getX() + 30, this.getY() + 145, 0x404040, false);
            if ((currentPage + 1) * ITEMS_PER_PAGE < totalItems) guiGraphics.drawString(Minecraft.getInstance().font, ">", this.getX() + 110, this.getY() + 145, 0x404040, false);
        }
    }

    private void renderItemDetails(GuiGraphics guiGraphics) {
        if (selectedItem.isEmpty()) return;
        int rightX = this.getX() + 160;
        int topY = this.getY() + 20;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(rightX + 60, topY + 30, 0);
        guiGraphics.pose().scale(2.5f, 2.5f, 2.5f);
        guiGraphics.renderFakeItem(selectedItem, -8, -8);
        guiGraphics.pose().popPose();

        Component name = selectedItem.getHoverName();
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, name, rightX + 60, topY + 55, 0x000000);
        guiGraphics.fill(rightX + 10, topY + 68, rightX + 110, topY + 69, 0xFF6B4E38);

        ElementProfileManager.getInstance().getElementProfile(selectedItem.getItem()).ifPresent(profile -> {
            int elementY = topY + 75;
            for (var entry : profile.elements().entrySet()) {
                Element element = AARegistries.ELEMENT_REGISTRY.get(entry.getKey());
                if (element != null) {
                    guiGraphics.blit(element.getIcon(), rightX + 15, elementY - 2, 0, 0, 12, 12, 12, 12);
                    String text = String.format("%s: %.1f", Component.translatable(element.getDescriptionId()).getString(), entry.getValue());
                    guiGraphics.drawString(Minecraft.getInstance().font, text, rightX + 32, elementY, 0x333333, false);
                    elementY += 14;
                }
            }
        });
    }

    private void renderElementDetails(GuiGraphics guiGraphics) {
        if (selectedElement == null) return;
        int rightX = this.getX() + 160;
        int topY = this.getY() + 20;

        // 大图标
        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(rightX + 60, topY + 30, 0);
        guiGraphics.pose().scale(2.5f, 2.5f, 2.5f);
        guiGraphics.blit(selectedElement.getIcon(), -8, -8, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();

        // 名称
        Component name = Component.translatable(selectedElement.getDescriptionId());
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, name, rightX + 60, topY + 55, 0x000000);
        guiGraphics.fill(rightX + 10, topY + 68, rightX + 110, topY + 69, 0xFF6B4E38);
        
        // 简单描述
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, Component.literal("This is a placeholder description for element " + name.getString()), rightX + 10, topY + 75, 100, 0x333333);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;

        if (toolbar.mouseClicked(mouseX, mouseY, button)) return true;

        int startX = this.getX() + 15;
        int startY = this.getY() + 30;
        
        int totalItems = (currentMode == DisplayMode.ITEMS) ? filteredItems.size() : filteredElements.size();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                if (currentMode == DisplayMode.ITEMS) {
                    this.selectedItem = filteredItems.get(i);
                } else {
                    this.selectedElement = filteredElements.get(i);
                }
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        
        // 翻页逻辑
        if (mouseY >= this.getY() + 140 && mouseY <= this.getY() + 160) {
            if (mouseX >= this.getX() + 20 && mouseX <= this.getX() + 50 && currentPage > 0) {
                currentPage--;
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (mouseX >= this.getX() + 90 && mouseX <= this.getX() + 120 && (currentPage + 1) * ITEMS_PER_PAGE < totalItems) {
                currentPage++;
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (toolbar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (toolbar.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
