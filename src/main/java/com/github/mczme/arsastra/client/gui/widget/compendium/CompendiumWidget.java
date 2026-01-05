package com.github.mczme.arsastra.client.gui.widget.compendium;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompendiumWidget extends AbstractWidget {
    private final PlayerKnowledge knowledge;
    private final CompendiumFilterWidget filterBar;
    
    private List<ItemStack> allAnalyzedItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private ItemStack selectedItem = ItemStack.EMPTY;
    
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 20;

    public CompendiumWidget(int x, int y, int width, int height, PlayerKnowledge knowledge) {
        super(x, y, width, height, Component.empty());
        this.knowledge = knowledge;
        this.filterBar = new CompendiumFilterWidget(x, y, this::onFilterChanged);
        refreshItems();
    }

    public void refreshItems() {
        this.allAnalyzedItems = ElementProfileManager.getInstance().getAllProfiledItems().stream()
                .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)))
                .filter(stack -> knowledge.hasAnalyzed(stack.getItem()))
                .collect(Collectors.toList());
        applyFilter(0);
    }

    private void onFilterChanged(int filterIndex) {
        applyFilter(filterIndex);
        this.currentPage = 0;
    }

    private void applyFilter(int filterIndex) {
        // 目前仅实现了简单的“全部”过滤，后续根据分类标签增加逻辑
        this.filteredItems = new ArrayList<>(allAnalyzedItems);
        
        if (selectedItem.isEmpty() && !filteredItems.isEmpty()) {
            selectedItem = filteredItems.get(0);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        filterBar.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGrid(guiGraphics, mouseX, mouseY);
        renderDetails(guiGraphics);
    }

    private void renderGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x40AA8800); // 选中高亮
            } else if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x20000000); // 悬停高亮
            }

            guiGraphics.renderFakeItem(stack, slotX, slotY);
        }
        
        // 分页器
        if (filteredItems.size() > ITEMS_PER_PAGE) {
            String pageText = (currentPage + 1) + " / " + (int) Math.ceil(filteredItems.size() / (double) ITEMS_PER_PAGE);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, pageText, this.getX() + 70, this.getY() + 145, 0x404040);
            
            // 简单的翻页箭头按钮
            if (currentPage > 0) guiGraphics.drawString(Minecraft.getInstance().font, "<", this.getX() + 30, this.getY() + 145, 0x404040, false);
            if ((currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) guiGraphics.drawString(Minecraft.getInstance().font, ">", this.getX() + 110, this.getY() + 145, 0x404040, false);
        }
    }

    private void renderDetails(GuiGraphics guiGraphics) {
        if (selectedItem.isEmpty()) return;
        
        int rightX = this.getX() + 160;
        int topY = this.getY() + 20;

        // 渲染大图标
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
                String elementName = Component.translatable("element." + entry.getKey().getNamespace() + "." + entry.getKey().getPath().replace("/", ".")).getString();
                String text = String.format("%s: %.1f", elementName, entry.getValue());
                guiGraphics.drawString(Minecraft.getInstance().font, text, rightX + 15, elementY, 0x333333, false);
                elementY += 10;
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;

        if (filterBar.mouseClicked(mouseX, mouseY, button)) return true;

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

            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                this.selectedItem = filteredItems.get(i);
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        
        // 翻页点击逻辑
        if (mouseY >= this.getY() + 140 && mouseY <= this.getY() + 160) {
            if (mouseX >= this.getX() + 20 && mouseX <= this.getX() + 50 && currentPage > 0) {
                currentPage--;
                return true;
            }
            if (mouseX >= this.getX() + 90 && mouseX <= this.getX() + 120 && (currentPage + 1) * ITEMS_PER_PAGE < filteredItems.size()) {
                currentPage++;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
