package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * 手稿筛选组件
 * 提供基于图标、物品和效果的筛选功能。
 */
public class ManuscriptFilterWidget extends ToolbarExpandableWidget {
    private static final ResourceLocation MANUSCRIPT_ICONS = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_icons.png");
    
    private final Runnable onFilterChanged;
    
    // 状态
    private int currentTab = 0; // 0: 图标, 1: 物品, 2: 效果
    private final Set<Integer> selectedIcons = new HashSet<>();
    private final EditBox itemInput;
    private final EditBox effectInput;
    
    // 动态尺寸常量
    private static final int WIDTH = 120;
    private static final int HEIGHT_ICONS = 125;
    private static final int HEIGHT_INPUT = 65; // 稍微增加高度以容纳标签
    
    // 本地化组件
    private static final Component[] TAB_TITLES = {
        Component.translatable("gui.ars_astra.filter.tab.icon"),
        Component.translatable("gui.ars_astra.filter.tab.item"),
        Component.translatable("gui.ars_astra.filter.tab.effect")
    };
    
    public ManuscriptFilterWidget(int x, int y, Runnable onFilterChanged) {
        super(x, y, WIDTH, HEIGHT_ICONS, 1, 0xA0A0A0); // 索引 1 是漏斗图标
        this.onFilterChanged = onFilterChanged;
        
        var font = Minecraft.getInstance().font;
        
        this.itemInput = new EditBox(font, 0, 0, 100, 14, TAB_TITLES[1]);
        this.itemInput.setMaxLength(50);
        this.itemInput.setBordered(true); 
        this.itemInput.setHint(Component.translatable("gui.ars_astra.filter.item_hint"));
        this.itemInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });
        
        this.effectInput = new EditBox(font, 0, 0, 100, 14, TAB_TITLES[2]);
        this.effectInput.setMaxLength(50);
        this.effectInput.setBordered(true);
        this.effectInput.setHint(Component.translatable("gui.ars_astra.filter.effect_hint"));
        this.effectInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });

        this.itemInput.setVisible(false);
        this.effectInput.setVisible(false);
    }

    public Set<Integer> getSelectedIcons() {
        return selectedIcons;
    }

    public String getItemFilter() {
        return itemInput.getValue();
    }

    public String getEffectFilter() {
        return effectInput.getValue();
    }

    @Override
    protected void onExpand() {
        updateVisibility();
    }
    
    @Override
    protected void onCollapse() {
        this.itemInput.setVisible(false);
        this.effectInput.setVisible(false);
        this.itemInput.setFocused(false);
        this.effectInput.setFocused(false);
    }
    
    /**
     * 更新组件可见性并根据当前标签页调整高度。
     */
    private void updateVisibility() {
        if (!expanded) return;
        this.itemInput.setVisible(currentTab == 1);
        this.effectInput.setVisible(currentTab == 2);
        
        // 根据标签页动态更新弹窗高度
        this.popupHeight = (currentTab == 0) ? HEIGHT_ICONS : HEIGHT_INPUT;
    }

    @Override
    protected void updatePopupLayout() {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        
        // 居中输入框 (宽度 120, 输入框 100 -> 偏移 10)
        // 下移以容纳标签
        if (itemInput != null) {
            this.itemInput.setX(bgX + 10);
            this.itemInput.setY(bgY + 40);
        }
        if (effectInput != null) {
            this.effectInput.setX(bgX + 10);
            this.effectInput.setY(bgY + 40);
        }
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        // 1. 渲染标签页
        renderTabs(guiGraphics, mouseX, mouseY, bgX, bgY);
        
        // 2. 根据当前标签渲染内容
        int contentY = bgY + 25;
        
        if (currentTab == 0) {
            // 网格居中：宽度 120。4 列 * 24px = 96px。边距 = (120 - 96) / 2 = 12px。
            renderIconGrid(guiGraphics, mouseX, mouseY, bgX + 12, contentY + 5);
        } else if (currentTab == 1) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.item_label"), bgX + 10, contentY + 2, 0xAAAAAA, false);
            itemInput.render(guiGraphics, mouseX, mouseY, partialTick);
        } else if (currentTab == 2) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.effect_label"), bgX + 10, contentY + 2, 0xAAAAAA, false);
            effectInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
    /**
     * 渲染弹窗顶部的标签页切换按钮。
     */
    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        int tabW = 40; // 120 / 3
        int tabH = 16;
        int startX = x;
        
        for (int i = 0; i < 3; i++) {
            int tx = startX + i * tabW;
            boolean active = (i == currentTab);
            boolean hovered = mouseX >= tx && mouseX < tx + tabW && mouseY >= y + 5 && mouseY < y + 5 + tabH;
            
            int color = active ? 0xFFFFFFFF : (hovered ? 0xFFAAAAAA : 0xFF555555);
            
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, TAB_TITLES[i], tx + tabW / 2, y + 9, color);
            
            // 选中项下方绘制白线
            if (active) {
                guiGraphics.fill(tx + 5, y + 20, tx + tabW - 5, y + 21, 0xFFFFFFFF);
            }
        }
    }
    
    /**
     * 渲染手稿图标选择网格。
     */
    private void renderIconGrid(GuiGraphics guiGraphics, int mouseX, int mouseY, int startX, int startY) {
        RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
        
        // 循环 0-14 (共 15 个图标)
        for (int i = 0; i < 15; i++) {
            int col = i % 4;
            int row = i / 4;
            int ix = startX + col * 24;
            int iy = startY + row * 24;
            
            boolean selected = selectedIcons.contains(i);
            boolean hovered = mouseX >= ix && mouseX < ix + 20 && mouseY >= iy && mouseY < iy + 20;
            
            if (selected) {
                guiGraphics.fill(ix - 2, iy - 2, ix + 18, iy + 18, 0xFF55FF55); // 绿色高亮
            } else if (hovered) {
                guiGraphics.fill(ix - 2, iy - 2, ix + 18, iy + 18, 0x40FFFFFF);
            }
            
            int u = (i % 4) * 16;
            int v = (i / 4) * 16;
            guiGraphics.blit(MANUSCRIPT_ICONS, ix, iy, u, v, 16, 16, 64, 64);
        }
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        
        // 检查标签页点击
        int tabW = 40;
        int tabH = 16;
        int startX = bgX;
        
        if (mouseY >= bgY + 5 && mouseY <= bgY + 5 + tabH) {
            for (int i = 0; i < 3; i++) {
                int tx = startX + i * tabW;
                if (mouseX >= tx && mouseX < tx + tabW) {
                    currentTab = i;
                    updateVisibility();
                    return true;
                }
            }
        }
        
        // 内容区域交互
        if (currentTab == 0) {
            // 图标网格点击
            int gridX = bgX + 12;
            int gridY = bgY + 30;
            for (int i = 0; i < 15; i++) {
                int col = i % 4;
                int row = i / 4;
                int ix = gridX + col * 24;
                int iy = gridY + row * 24;
                
                if (mouseX >= ix && mouseX < ix + 20 && mouseY >= iy && mouseY < iy + 20) {
                    if (selectedIcons.contains(i)) {
                        selectedIcons.remove(i);
                    } else {
                        selectedIcons.add(i);
                    }
                    if (onFilterChanged != null) onFilterChanged.run();
                    return true;
                }
            }
        } else if (currentTab == 1) {
            if (itemInput.mouseClicked(mouseX, mouseY, button)) {
                itemInput.setFocused(true);
                return true;
            } else {
                itemInput.setFocused(false);
            }
        } else if (currentTab == 2) {
            if (effectInput.mouseClicked(mouseX, mouseY, button)) {
                effectInput.setFocused(true);
                return true;
            } else {
                effectInput.setFocused(false);
            }
        }
        
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (keyCode == 256) { // ESC
                setExpanded(false);
                return true;
            }
            if (currentTab == 1 && itemInput.isFocused() && itemInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (currentTab == 2 && effectInput.isFocused() && effectInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            return true; 
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            if (currentTab == 1 && itemInput.isFocused()) return itemInput.charTyped(codePoint, modifiers);
            if (currentTab == 2 && effectInput.isFocused()) return effectInput.charTyped(codePoint, modifiers);
        }
        return false;
    }
}