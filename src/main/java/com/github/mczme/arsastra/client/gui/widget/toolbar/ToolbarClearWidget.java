package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ToolbarClearWidget extends ToolbarExpandableWidget {
    private final Runnable onConfirm;
    private Button confirmButton;

    public ToolbarClearWidget(int x, int y, Runnable onConfirm) {
        // 图标索引 7 (橡皮擦), 红色 (0xFF5555), 弹出框大小 80x40
        super(x, y, 80, 40, 7, 0xFF5555);
        this.onConfirm = onConfirm;
        this.setExpandDirection(ExpandDirection.LEFT);
        
        this.confirmButton = Button.builder(Component.translatable("gui.ars_astra.workshop.clear.confirm"), (btn) -> {
            if (this.onConfirm != null) this.onConfirm.run();
            setExpanded(false);
        }).bounds(0, 0, 70, 20).build();
        
        updatePopupLayout();
    }

    @Override
    protected void updatePopupLayout() {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        if (confirmButton != null) {
            confirmButton.setX(bgX + 5);
            confirmButton.setY(bgY + 10);
        }
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        return confirmButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    protected void onCollapse() {
        confirmButton.setFocused(false);
    }
}
