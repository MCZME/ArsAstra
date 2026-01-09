package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopViewModel;
import com.github.mczme.arsastra.client.gui.widget.ConfirmationDialog;
import com.github.mczme.arsastra.client.gui.widget.workshop.*;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class WorkshopTab implements JournalTab, DragHandler {
    private WorkshopCanvasWidget canvasWidget;
    private SourceFloatingPanel sourcePanel;
    private SequenceStripWidget sequenceStrip;
    private WorkshopToolbar toolbar;
    private WorkshopViewModel viewModel;
    private ConfirmationDialog clearConfirmDialog;
    private InfoPanelWidget infoPanel;
    
    private ItemStack draggingStack = ItemStack.EMPTY;
    private boolean isDragging = false;
    
    // 当前正在交互的组件
    private GuiEventListener activeWidget = null;

    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        if (this.viewModel == null) {
            this.viewModel = new WorkshopViewModel();
        }
        
        PlayerKnowledge knowledge = screen.getPlayerKnowledge();

        // 1. 画布 (最底层)
        this.canvasWidget = new WorkshopCanvasWidget(x + 10, y + 10, width - 20, height - 20, this, viewModel);
        this.canvasWidget.setKnowledge(knowledge);
        this.canvasWidget.visible = false;
        screen.addTabWidget(this.canvasWidget);

        // 2. 清空确认弹窗 (逻辑上最顶层)
        this.clearConfirmDialog = new ConfirmationDialog(x + width/2 - 80, y + height/2 - 50, 160, 100, 
            Component.translatable("gui.ars_astra.workshop.clear.title"), 
            Component.translatable("gui.ars_astra.workshop.clear.message"),
            () -> {
                viewModel.clear();
                clearConfirmDialog.visible = false;
            },
            () -> clearConfirmDialog.visible = false
        );
        this.clearConfirmDialog.visible = false;
        
        // 3. 辅助面板 (Info)
        // 信息面板 (右上角下方)
        this.infoPanel = new InfoPanelWidget(x + width - 180, y + 25, 150, 80);
        this.infoPanel.visible = false;
        screen.addTabWidget(this.infoPanel);

        // 4. 工具栏
        this.toolbar = new WorkshopToolbar(x + 15, y - 13, 270, 22, viewModel, new WorkshopActionHandler() {
            @Override
            public void onFilterChanged() {
                if (sourcePanel != null && toolbar != null) {
                    sourcePanel.updateFilter(toolbar.getSearchQuery(), toolbar.getElementFilter(), toolbar.getTagFilter());
                }
            }

            @Override
            public void onClearRequest() {
                clearConfirmDialog.visible = true;
            }

            @Override
            public void onInfoToggle() {
                infoPanel.visible = !infoPanel.visible;
            }

            @Override
            public void onSettingsToggle() {
                // Handled internally by toolbar widget
            }

            @Override
            public void onSaveRequest() {
                // TODO: Save implementation
            }

            @Override
            public void onChartTypeChanged(String type) {
                // TODO: 切换星图类型逻辑
            }
        });
        this.toolbar.visible = false;
        screen.addTabWidget(this.toolbar);
        
        // 5. 序列条
        this.sequenceStrip = new SequenceStripWidget(x + 15, y + height - 45, width - 30, viewModel, this);
        this.sequenceStrip.visible = false;
        screen.addTabWidget(this.sequenceStrip);

        // 6. 原料面板
        this.sourcePanel = new SourceFloatingPanel(x + 15, y + 15, knowledge, this);
        this.sourcePanel.visible = false;
        screen.addTabWidget(this.sourcePanel);
    }

    @Override
    public void startDrag(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.draggingStack = stack.copy();
            this.isDragging = true;
        }
    }

    @Override
    public boolean isDragging() {
        return isDragging;
    }

    @Override
    public ItemStack getDraggingStack() {
        return draggingStack;
    }

    @Override
    public void endDrag() {
        this.draggingStack = ItemStack.EMPTY;
        this.isDragging = false;
    }
    
    public void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isDragging && !draggingStack.isEmpty()) {
            guiGraphics.renderFakeItem(draggingStack, mouseX - 8, mouseY - 8);
        }
        
        // 渲染模态弹窗 (位于最顶层)
        if (clearConfirmDialog != null && clearConfirmDialog.visible) {
            guiGraphics.fill(0, 0, 10000, 10000, 0x80000000);
            clearConfirmDialog.render(guiGraphics, mouseX, mouseY, 0);
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ConfirmationDialog handles its own rendering in renderOverlay to be on top of everything
    }

    @Override
    public void setVisible(boolean visible) {
        if (canvasWidget != null) canvasWidget.visible = visible;
        if (sourcePanel != null) sourcePanel.visible = visible;
        if (sequenceStrip != null) sequenceStrip.visible = visible;
        if (toolbar != null) toolbar.visible = visible;
        
        if (visible) {
            if (infoPanel != null) infoPanel.visible = false;
            if (clearConfirmDialog != null) clearConfirmDialog.visible = false;
        } else {
            if (infoPanel != null) infoPanel.visible = false;
            if (clearConfirmDialog != null) clearConfirmDialog.visible = false;
        }

        if (!visible) activeWidget = null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 优先处理模态弹窗
        if (clearConfirmDialog != null && clearConfirmDialog.visible) {
            if (clearConfirmDialog.mouseClicked(mouseX, mouseY, button)) return true;
            return true; // 模态拦截
        }
        
        activeWidget = null;
        
        // 处理悬浮面板点击 (按 Z 轴倒序检查)
        
        // 1. Info Panel
        if (infoPanel != null && infoPanel.visible) {
            if (infoPanel.mouseClicked(mouseX, mouseY, button)) {
                activeWidget = infoPanel;
                return true;
            }
        }

        // 2. Toolbar
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY, button)) { activeWidget = toolbar; return true; }
        
        // 3. Source Panel
        if (sourcePanel != null && sourcePanel.mouseClicked(mouseX, mouseY, button)) { activeWidget = sourcePanel; return true; }
        
        // 4. Sequence Strip
        if (sequenceStrip != null && sequenceStrip.mouseClicked(mouseX, mouseY, button)) { activeWidget = sequenceStrip; return true; }
        
        // 5. Canvas (Background)
        if (canvasWidget != null && canvasWidget.mouseClicked(mouseX, mouseY, button)) { activeWidget = canvasWidget; return true; }
        
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        
        if (clearConfirmDialog != null && clearConfirmDialog.visible) {
            return true;
        }

        // Special handling for Drag & Drop
        if (isDragging) {
            // Check potential drop targets in reverse Z-order (topmost first)
            if (toolbar != null && toolbar.isMouseOver(mouseX, mouseY)) {
                if (toolbar.mouseReleased(mouseX, mouseY, button)) handled = true;
            }
            else if (sourcePanel != null && sourcePanel.isMouseOver(mouseX, mouseY)) {
                if (sourcePanel.mouseReleased(mouseX, mouseY, button)) handled = true;
            }
            else if (sequenceStrip != null && sequenceStrip.isMouseOver(mouseX, mouseY)) {
                if (sequenceStrip.mouseReleased(mouseX, mouseY, button)) handled = true;
            }
            else if (canvasWidget != null && canvasWidget.isMouseOver(mouseX, mouseY)) {
                if (canvasWidget.mouseReleased(mouseX, mouseY, button)) handled = true;
            }

            if (!handled && isDragging) {
                endDrag();
            }
            
            activeWidget = null;
            return true;
        }

        // Standard Click Release
        if (activeWidget != null) {
            handled = activeWidget.mouseReleased(mouseX, mouseY, button);
            activeWidget = null;
        } else {
            // Fallback dispatch
            if (infoPanel != null && infoPanel.mouseReleased(mouseX, mouseY, button)) handled = true;
            if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY, button)) handled = true;
            if (sourcePanel != null && sourcePanel.mouseReleased(mouseX, mouseY, button)) handled = true;
            if (sequenceStrip != null && sequenceStrip.mouseReleased(mouseX, mouseY, button)) handled = true;
            if (canvasWidget != null && canvasWidget.mouseReleased(mouseX, mouseY, button)) handled = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeWidget != null) {
            return activeWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (clearConfirmDialog != null && clearConfirmDialog.visible) return true; // 模态拦截
        
        if (infoPanel != null && infoPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        
        if (toolbar != null && toolbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (sourcePanel != null && sourcePanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (sequenceStrip != null && sequenceStrip.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (canvasWidget != null && canvasWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (toolbar != null && toolbar.charTyped(codePoint, modifiers)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (toolbar != null && toolbar.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }
    
    public WorkshopCanvasWidget getCanvasWidget() {
        return canvasWidget;
    }
    
    public WorkshopViewModel getViewModel() {
        return viewModel;
    }
}
