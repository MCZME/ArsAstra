package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.ItemFilterLogic;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.registry.AARegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceFloatingPanel extends FloatingWidget {
    private final PlayerKnowledge knowledge;
    private final DragHandler dragHandler;
    private SourceMode mode = SourceMode.INVENTORY;
    private final List<ItemStack> displayItems = new ArrayList<>();
    
    // 过滤状态
    private String searchQuery = "";
    private String elementFilter = "";
    private String tagFilter = "";
    
    // Grid settings
    private static final int COLS = 4;
    private static final int ROWS = 5; // Increased to 5
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 2;

    public SourceFloatingPanel(int x, int y, PlayerKnowledge knowledge, DragHandler dragHandler) {
        super(x, y, 100, 130, Component.translatable("gui.ars_astra.workshop.source")); // Adjusted height
        this.knowledge = knowledge;
        this.dragHandler = dragHandler;
        refreshItems();
    }
    
    public void updateFilter(String searchQuery, String elementFilter, String tagFilter) {
        this.searchQuery = searchQuery.toLowerCase();
        this.elementFilter = elementFilter;
        this.tagFilter = tagFilter;
        refreshItems();
    }

    private void refreshItems() {
        displayItems.clear();
        Predicate<ItemStack> advancedFilter = ItemFilterLogic.create(elementFilter, tagFilter);
        Predicate<ItemStack> searchPredicate = stack -> searchQuery.isEmpty() || stack.getHoverName().getString().toLowerCase().contains(searchQuery);
        
        Stream<ItemStack> itemStream;
        
        if (mode == SourceMode.INVENTORY) {
            Inventory inv = Minecraft.getInstance().player.getInventory();
            List<ItemStack> invItems = new ArrayList<>();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) invItems.add(stack);
            }
            itemStream = invItems.stream();
        } else {
            // COMPENDIUM mode
            itemStream = (knowledge == null) ? Stream.empty() : 
                knowledge.getAnalyzedItems().stream()
                    .map(id -> BuiltInRegistries.ITEM.get(id))
                    .filter(item -> item != Items.AIR)
                    .map(ItemStack::new);
        }

        this.displayItems.addAll(itemStream
            .filter(advancedFilter)
            .filter(searchPredicate)
            .limit(COLS * ROWS)
            .collect(Collectors.toList()));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 背景 (羊皮纸色)
        // 绘制不透明底色
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Palette.PARCHMENT_BG);
        
        // 绘制双层边框
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Palette.BORDER_INK);
        guiGraphics.renderOutline(getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4, Palette.BORDER_DECOR); // 内装饰线

        // 2. 标题栏渲染
        int titleHeight = 14;
        Component title = getMessage();
        
        // 绘制标题文字（左侧）
        guiGraphics.drawString(Minecraft.getInstance().font, title, getX() + 6, getY() + 4, Palette.INK, false);
        
        // --- 绘制模式切换按钮 ---
        renderSwitchButton(guiGraphics, mouseX, mouseY);
        
        // 分割线
        guiGraphics.fill(getX() + 4, getY() + titleHeight, getX() + getWidth() - 4, getY() + titleHeight + 1, Palette.INK);

        // 3. 物品网格
        int startX = getX() + 10;
        int startY = getY() + 20; // 紧贴标题栏下方
        
        for (int i = 0; i < COLS * ROWS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int slotX = startX + col * (SLOT_SIZE + PADDING);
            int slotY = startY + row * (SLOT_SIZE + PADDING);

            // 绘制槽位背景 (淡墨色轮廓)
            guiGraphics.renderOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE, Palette.INK);

            // 绘制物品
            if (i < displayItems.size()) {
                ItemStack stack = displayItems.get(i);
                guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1);
            }

            // 悬停高亮与 Tooltip
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                guiGraphics.renderOutline(slotX - 1, slotY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Palette.CINNABAR);
                if (i < displayItems.size()) {
                    ItemStack stack = displayItems.get(i);
                    List<Component> tooltipLines = new ArrayList<>();
                    tooltipLines.add(stack.getHoverName());
                    
                    ElementProfileManager.getInstance().getElementProfile(stack.getItem()).ifPresent(profile -> {
                        for (Map.Entry<ResourceLocation, Float> entry : profile.elements().entrySet()) {
                            Element element = AARegistries.ELEMENT_REGISTRY.get(entry.getKey());
                            if (element != null) {
                                tooltipLines.add(Component.literal("  ")
                                    .append(Component.translatable(element.getDescriptionId()))
                                    .append(": " + entry.getValue())
                                    .withStyle(ChatFormatting.GRAY));
                            }
                        }
                    });
                    
                    guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, tooltipLines, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * 绘制切换按钮（纯色色块 + 阴影风格）
     */
    private void renderSwitchButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String switchText = (mode == SourceMode.INVENTORY) ? "Inv" : "Book";
        int switchTextWidth = Minecraft.getInstance().font.width(switchText);
        int btnPadding = 4;
        int btnWidth = switchTextWidth + btnPadding * 2;
        int btnHeight = 11; // 增加高度
        
        // 按钮位置
        int btnX = getX() + getWidth() - btnWidth - 6;
        int btnY = getY() + 3;

        boolean isHoveringBtn = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        
        int currentBg = isHoveringBtn ? Palette.BTN_HOVER : Palette.BTN_NORMAL;
        int currentText = isHoveringBtn ? Palette.BTN_TEXT_HOVER : Palette.BTN_TEXT;

        // 1. 绘制主体色块
        guiGraphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, currentBg);
        
        // 2. 绘制右侧和底部阴影 (模拟厚度)
        // 右侧阴影
        guiGraphics.fill(btnX + btnWidth, btnY + 1, btnX + btnWidth + 1, btnY + btnHeight + 1, Palette.BTN_SHADOW);
        // 底部阴影
        guiGraphics.fill(btnX + 1, btnY + btnHeight, btnX + btnWidth + 1, btnY + btnHeight + 1, Palette.BTN_SHADOW);

        // 3. 绘制文字 (居中)
        guiGraphics.drawString(Minecraft.getInstance().font, switchText, btnX + btnPadding, btnY + 2, currentText, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle Toggle Click (Title Bar Right)
        if (mouseY >= getY() && mouseY <= getY() + 14) {
            // Check if clicked on the right side of the title bar (approx area)
            if (mouseX >= getX() + getWidth() - 40 && mouseX <= getX() + getWidth()) {
                mode = (mode == SourceMode.INVENTORY) ? SourceMode.COMPENDIUM : SourceMode.INVENTORY;
                refreshItems();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        
        // Handle Item Click (Drag Start)
        int startX = getX() + 10;
        int startY = getY() + 20;
        
        for (int i = 0; i < displayItems.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int slotX = startX + col * (SLOT_SIZE + PADDING);
            int slotY = startY + row * (SLOT_SIZE + PADDING);
            
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                if (button == 0 && !dragHandler.isDragging()) {
                    dragHandler.startDrag(displayItems.get(i));
                    return true;
                }
            }
        }

        // Fallback to super (Window Dragging & Interception)
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum SourceMode {
        INVENTORY,
        COMPENDIUM
    }
}