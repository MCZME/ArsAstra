package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToolbarSettingsWidget extends ToolbarExpandableWidget {
    private final Consumer<String> onStarChartChanged;
    
    private Button baseChartButton;
    private Button comingSoonButton;

    public ToolbarSettingsWidget(int x, int y, Consumer<String> onStarChartChanged) {
        super(x, y, 120, 60, 10, 0x606060);
        this.onStarChartChanged = onStarChartChanged;
        
        this.baseChartButton = Button.builder(Component.literal("Base Chart"), (btn) -> {
            if (this.onStarChartChanged != null) {
                this.onStarChartChanged.accept("base_chart");
            }
            setExpanded(false); 
        }).bounds(0, 0, 100, 20).build();

        this.comingSoonButton = Button.builder(Component.literal("Coming Soon..."), (btn) -> {}).bounds(0, 0, 100, 20).build();
        this.comingSoonButton.active = false;

        updatePopupLayout();
    }

    @Override
    protected void onCollapse() {
        this.baseChartButton.setFocused(false);
        this.comingSoonButton.setFocused(false);
    }

    @Override
    protected void updatePopupLayout() {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        
        if (baseChartButton != null) {
            baseChartButton.setX(bgX + 10);
            baseChartButton.setY(bgY + 10);
        }
        if (comingSoonButton != null) {
            comingSoonButton.setX(bgX + 10);
            comingSoonButton.setY(bgY + 35);
        }
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        baseChartButton.render(guiGraphics, mouseX, mouseY, partialTick);
        comingSoonButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        if (baseChartButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (comingSoonButton.mouseClicked(mouseX, mouseY, button)) return true;
        return false;
    }
}