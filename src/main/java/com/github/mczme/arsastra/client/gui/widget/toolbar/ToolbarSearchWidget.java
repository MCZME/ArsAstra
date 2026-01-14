package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToolbarSearchWidget extends ToolbarSideExpandableWidget {
    private final SearchBox searchBox;

    public ToolbarSearchWidget(int x, int y, Consumer<String> onSearch) {
        super(x, y, 100, ExpandDirection.RIGHT, 0, 0xA08030);
        
        this.searchBox = new SearchBox(Minecraft.getInstance().font, 10, 16, Component.empty());
        this.searchBox.setResponder(onSearch);
        this.searchBox.setVisible(false);
        this.searchBox.setBordered(false);
    }

    @Override
    protected void onExpand() {
        this.searchBox.setVisible(true);
    }

    @Override
    protected void onCollapse() {
        this.searchBox.setFocused(false);
    }

    @Override
    protected void updateContentLayout(int x, int y, int width, int height) {
        int boxX = x + 20;
        int boxY = y + (height - 16) / 2 + 4; 
        int boxW = Math.max(0, width - 24);
        
        this.searchBox.setX(boxX);
        this.searchBox.setY(boxY);
        this.searchBox.setWidth(boxW);
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;

        if (this.isHovered()) {
            if (expanded) {
                if (mouseX < this.getX() + 20) {
                    this.toggleExpand();
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                } else {
                    this.searchBox.setFocused(true);
                    if (this.searchBox.isMouseOver(mouseX, mouseY)) {
                        this.searchBox.mouseClicked(mouseX, mouseY, button);
                    }
                }
            } else {
                this.toggleExpand();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        
        if (expanded) {
             if (searchBox.getValue().isEmpty()) {
                setExpanded(false);
            } else {
                searchBox.setFocused(false);
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

            if (searchBox.isFocused()) {
                if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (keyCode == 257 || keyCode == 335) return true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expanded) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    private static class SearchBox extends EditBox {
        public SearchBox(Font font, int width, int height, Component message) {
            super(font, 0, 0, width, height, message);
            this.setBordered(false);
            this.setTextColor(0x404040);
            this.setMaxLength(32);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int h = getHeight();

            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

            if (this.getValue().isEmpty() && !this.isFocused()) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.journal.compendium.search"), x + 1, y + (h - 9) / 2 - 3, 0x999999, false);
            }
        }
    }
}