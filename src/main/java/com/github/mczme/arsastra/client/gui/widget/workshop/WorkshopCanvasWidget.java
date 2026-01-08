package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopViewModel;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WorkshopCanvasWidget extends StarChartWidget {
    private final DragHandler dragHandler;
    private final WorkshopViewModel viewModel;

    public WorkshopCanvasWidget(int x, int y, int width, int height, DragHandler dragHandler, WorkshopViewModel viewModel) {
        super(x, y, width, height, Component.empty());
        this.dragHandler = dragHandler;
        this.viewModel = viewModel;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 从 ViewModel 同步最新的预测数据
        if (this.viewModel != null) {
            this.setPrediction(this.viewModel.getPredictionPath(), this.viewModel.getStability());
        }
        
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        // Future: Add drag and drop highlights or specific workshop overlays here
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && dragHandler.isDragging()) {
            viewModel.addToSequence(dragHandler.getDraggingStack());
            dragHandler.endDrag();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
