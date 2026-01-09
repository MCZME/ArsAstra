package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ToolbarFilterWidget extends ToolbarExpandableWidget {
    private final EditBox elementsInput;
    private final EditBox tagsInput;
    private final Runnable onFilterChanged;

    public ToolbarFilterWidget(int x, int y, Runnable onFilterChanged) {
        super(x, y, 140, 80, 1, 0xA0A0A0);
        this.onFilterChanged = onFilterChanged;
        
        var font = Minecraft.getInstance().font;
        this.elementsInput = new EditBox(font, 0, 0, 130, 14, Component.literal("Elements"));
        this.elementsInput.setMaxLength(50);
        this.elementsInput.setBordered(false);
        this.elementsInput.setTextColor(0xFFFFFF);
        this.elementsInput.setHint(Component.translatable("gui.ars_astra.filter.elements_hint"));
        this.elementsInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });
        
        this.tagsInput = new EditBox(font, 0, 0, 130, 14, Component.literal("Tags"));
        this.tagsInput.setMaxLength(50);
        this.tagsInput.setBordered(false);
        this.tagsInput.setTextColor(0xFFFFFF);
        this.tagsInput.setHint(Component.translatable("gui.ars_astra.filter.tags_hint"));
        this.tagsInput.setResponder(s -> {
            if (this.onFilterChanged != null) this.onFilterChanged.run();
        });

        this.elementsInput.setVisible(false);
        this.tagsInput.setVisible(false);
        
        updatePopupLayout();
    }

    @Override
    protected void onExpand() {
        this.elementsInput.setVisible(true);
        this.tagsInput.setVisible(true);
    }
    
    @Override
    protected void onCollapse() {
        this.elementsInput.setVisible(false);
        this.tagsInput.setVisible(false);
        this.elementsInput.setFocused(false);
        this.tagsInput.setFocused(false);
    }

    public String getElementFilter() {
        return elementsInput.getValue();
    }
    
    public String getTagFilter() {
        return tagsInput.getValue();
    }
    
    @Override
    protected void updatePopupLayout() {
        int bgY = this.getY() + 22;
        if (elementsInput != null) {
            this.elementsInput.setX(this.getX() + 5);
            this.elementsInput.setY(bgY + 18 + 3);
        }
        if (tagsInput != null) {
            this.tagsInput.setX(this.getX() + 5);
            this.tagsInput.setY(bgY + 48 + 3);
        }
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        int currentW = (int) (popupWidth * animationProgress);
        
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.elements_label"), bgX + 5, bgY + 5, 0xAAAAAA, false);
        // Box bg
        guiGraphics.fill(bgX + 4, bgY + 18, bgX + currentW - 4, bgY + 18 + 16, 0xFF000000);
        elementsInput.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.filter.tags_label"), bgX + 5, bgY + 38, 0xAAAAAA, false);
        // Box bg
        guiGraphics.fill(bgX + 4, bgY + 48, bgX + currentW - 4, bgY + 48 + 16, 0xFF000000);
        tagsInput.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        int bgY = this.getY() + 22;
                
        // 检查要素输入框区域
        if (mouseY >= bgY + 18 && mouseY <= bgY + 34) {
            elementsInput.setFocused(true);
            tagsInput.setFocused(false);
            elementsInput.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        
        // 检查标签输入框区域
        if (mouseY >= bgY + 48 && mouseY <= bgY + 64) {
            tagsInput.setFocused(true);
            elementsInput.setFocused(false);
            tagsInput.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        
        elementsInput.setFocused(false);
        tagsInput.setFocused(false);
        return false; // Content not handled, but fall through to background click
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (expanded) {
            if (keyCode == 256) {
                setExpanded(false);
                return true;
            }
            if (elementsInput.isFocused() && elementsInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (tagsInput.isFocused() && tagsInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            
            // Allow other keys but consume them to prevent global actions
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            if (elementsInput.isFocused()) return elementsInput.charTyped(codePoint, modifiers);
            if (tagsInput.isFocused()) return tagsInput.charTyped(codePoint, modifiers);
        }
        return false;
    }
}