package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarExpandableWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class ManuscriptFilterWidget extends ToolbarExpandableWidget {
    private static final ResourceLocation MANUSCRIPT_ICONS = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_icons.png");
    
    private final Runnable onFilterChanged;
    
    // State
    private int currentTab = 0; // 0: Icons, 1: Items, 2: Effects
    private final Set<Integer> selectedIcons = new HashSet<>();
    private final EditBox itemInput;
    private final EditBox effectInput;
    
    public ManuscriptFilterWidget(int x, int y, Runnable onFilterChanged) {
        super(x, y, 160, 130, 1, 0xA0A0A0); // Index 1 is filter icon
        this.onFilterChanged = onFilterChanged;
        
        var font = Minecraft.getInstance().font;
        
        this.itemInput = new EditBox(font, 0, 0, 140, 14, Component.literal("Item Filter"));
        this.itemInput.setMaxLength(50);
        this.itemInput.setBordered(true); 
        this.itemInput.setHint(Component.translatable("gui.ars_astra.filter.item_hint"));
        this.itemInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });
        
        this.effectInput = new EditBox(font, 0, 0, 140, 14, Component.literal("Effect Filter"));
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
    
    private void updateVisibility() {
        if (!expanded) return;
        this.itemInput.setVisible(currentTab == 1);
        this.effectInput.setVisible(currentTab == 2);
        
        if (currentTab == 1) this.itemInput.setFocused(true);
        else this.itemInput.setFocused(false);
        
        if (currentTab == 2) this.effectInput.setFocused(true);
        else this.effectInput.setFocused(false);
    }

    @Override
    protected void updatePopupLayout() {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        
        // Center inputs
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
        // 1. Render Tabs
        renderTabs(guiGraphics, bgX, bgY);
        
        // 2. Render Content based on tab
        int contentY = bgY + 25;
        
        if (currentTab == 0) {
            renderIconGrid(guiGraphics, mouseX, mouseY, bgX + 10, contentY + 10);
        } else if (currentTab == 1) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.item_label"), bgX + 10, contentY + 5, 0x404040, false);
            itemInput.render(guiGraphics, mouseX, mouseY, partialTick);
        } else if (currentTab == 2) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.effect_label"), bgX + 10, contentY + 5, 0x404040, false);
            effectInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
    
    private void renderTabs(GuiGraphics guiGraphics, int x, int y) {
        int tabW = 50;
        int tabH = 16;
        int startX = x + 5;
        
        String[] titles = {"Icon", "Item", "Effect"};
        
        for (int i = 0; i < 3; i++) {
            int tx = startX + i * tabW;
            boolean active = (i == currentTab);
            int color = active ? 0xFFFFFFFF : 0xFFAAAAAA;
            int bgColor = active ? 0xFF666666 : 0xFF333333;
            
            guiGraphics.fill(tx, y + 5, tx + tabW - 2, y + 5 + tabH, bgColor);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, titles[i], tx + tabW / 2 - 1, y + 9, color);
        }
    }
    
    private void renderIconGrid(GuiGraphics guiGraphics, int mouseX, int mouseY, int startX, int startY) {
        RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
        
        for (int i = 0; i < 16; i++) {
            int col = i % 4;
            int row = i / 4;
            int ix = startX + col * 24;
            int iy = startY + row * 24;
            
            boolean selected = selectedIcons.contains(i);
            boolean hovered = mouseX >= ix && mouseX < ix + 20 && mouseY >= iy && mouseY < iy + 20;
            
            if (selected) {
                guiGraphics.fill(ix - 2, iy - 2, ix + 18, iy + 18, 0xFF55FF55); // Green highlight
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
        
        // Check Tabs
        int tabW = 50;
        int tabH = 16;
        int startX = bgX + 5;
        
        if (mouseY >= bgY + 5 && mouseY <= bgY + 5 + tabH) {
            for (int i = 0; i < 3; i++) {
                int tx = startX + i * tabW;
                if (mouseX >= tx && mouseX < tx + tabW - 2) {
                    currentTab = i;
                    updateVisibility();
                    return true;
                }
            }
        }
        
        // Content interactions
        if (currentTab == 0) {
            // Grid clicks
            int gridX = bgX + 10;
            int gridY = bgY + 35;
            for (int i = 0; i < 16; i++) {
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
            if (itemInput.mouseClicked(mouseX, mouseY, button)) return true;
        } else if (currentTab == 2) {
            if (effectInput.mouseClicked(mouseX, mouseY, button)) return true;
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