package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WorkshopCanvasWidget extends StarChartWidget {
    private final DragHandler dragHandler;
    private final WorkshopSession session;

    public WorkshopCanvasWidget(int x, int y, int width, int height, DragHandler dragHandler, WorkshopSession session) {
        super(x, y, width, height, Component.empty());
        this.dragHandler = dragHandler;
        this.session = session;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 从 Session 同步最新的推演结果
        if (this.session != null) {
            this.setDeductionResult(this.session.getDeductionResult());
        }
        
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && dragHandler.isDragging()) {
            session.addInput(dragHandler.getDraggingStack());
            dragHandler.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
