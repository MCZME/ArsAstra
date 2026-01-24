package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.widget.workshop.*;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class WorkshopTab implements JournalTab, DragHandler {
    private WorkshopCanvasWidget canvasWidget;
    private SourceFloatingPanel sourcePanel;
    private SequenceStripWidget sequenceStrip;
    private WorkshopToolbar toolbar;
    private StickyNoteWidget stickyNote;
    private SelectionInfoCard selectionInfoCard;
    private WorkshopSession session;
    
    private ItemStack draggingStack = ItemStack.EMPTY;
    private boolean isDragging = false;
    private int dragSourceIndex = -1;
    
    // 当前正在交互的组件
    private GuiEventListener activeWidget = null;

    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        if (this.session == null) {
            // 默认加载基础星图，后续可根据玩家选择切换
            this.session = new WorkshopSession(ResourceLocation.fromNamespaceAndPath("ars_astra", "base_chart"));
        }
        
        // 注册第一个物品添加时的视角定位逻辑
        this.session.setOnFirstItemAddedListener(stack -> {
            ElementProfileManager.getInstance().getElementProfile(stack.getItem()).ifPresent(profile -> {
                if (canvasWidget != null) {
                    canvasWidget.centerOn(profile.launchPoint());
                }
            });
        });
        
        PlayerKnowledge knowledge = screen.getPlayerKnowledge();
        this.session.setKnowledge(knowledge);

        // RENDER ORDER: Bottom -> Top
        
        // 1. 画布 (最底层)
        this.canvasWidget = new WorkshopCanvasWidget(x + 10, y + 10, width - 20, height - 20, this, session);
        this.canvasWidget.setKnowledge(knowledge);
        this.canvasWidget.visible = false;
        screen.addTabWidget(this.canvasWidget);

        // 2. 序列条
        this.sequenceStrip = new SequenceStripWidget(x + 15, y + height - 45, width - 30, session, this);
        this.sequenceStrip.visible = false;
        screen.addTabWidget(this.sequenceStrip);
        
        // 3. 选中项详情卡片 (位于序列条上方)
        this.selectionInfoCard = new SelectionInfoCard(x + 15, y + height - 45, session);
        this.selectionInfoCard.visible = false;
        screen.addTabWidget(this.selectionInfoCard);

        // 4. 工具栏 (中层)
        this.toolbar = new WorkshopToolbar(x + 15, y - 13, 270, 22, session, 
            () -> { // onFilterChanged
                if (sourcePanel != null && toolbar != null) {
                    sourcePanel.updateFilter(toolbar.getSearchQuery(), toolbar.getElementFilter(), toolbar.getTagFilter());
                }
            },
            () -> { // onInfoToggle
                if (stickyNote != null) {
                    stickyNote.visible = !stickyNote.visible;
                }
            }
        );
        this.toolbar.visible = false;
        screen.addTabWidget(this.toolbar);

        // 5. 原料面板 (悬浮层)
        this.sourcePanel = new SourceFloatingPanel(x + 15, y + 15, knowledge, this);
        this.sourcePanel.visible = false;
        screen.addTabWidget(this.sourcePanel);

        // 6. 便签纸 (悬浮层 - 最顶层)
        this.stickyNote = new StickyNoteWidget(x + width - 150, y + 50, session);
        this.stickyNote.visible = false;
        screen.addTabWidget(this.stickyNote);
    }

    @Override
    public void startDrag(ItemStack stack, int sourceIndex) {
        if (!stack.isEmpty()) {
            this.draggingStack = stack.copy();
            this.isDragging = true;
            this.dragSourceIndex = sourceIndex;
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
    public int getDragSourceIndex() {
        return dragSourceIndex;
    }

    @Override
    public void endDrag() {
        this.draggingStack = ItemStack.EMPTY;
        this.isDragging = false;
        this.dragSourceIndex = -1;
    }
    
    public void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isDragging && !draggingStack.isEmpty()) {
            guiGraphics.renderFakeItem(draggingStack, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void setVisible(boolean visible) {
        if (canvasWidget != null) canvasWidget.visible = visible;
        if (sourcePanel != null) sourcePanel.visible = visible;
        if (sequenceStrip != null) sequenceStrip.visible = visible;
        if (toolbar != null) toolbar.visible = visible;
        if (selectionInfoCard != null) selectionInfoCard.visible = visible;
        // StickyNote visibility is managed by user toggle, but hidden when tab is hidden
        if (!visible && stickyNote != null) stickyNote.visible = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        activeWidget = null;
        
        // 0. Priority: Toolbar Popups (Topmost Visual Layer)
        if (toolbar != null && toolbar.handlePopupClick(mouseX, mouseY, button)) {
            activeWidget = toolbar;
            return true;
        }
        
        // HIT TEST ORDER: Top -> Bottom (Reverse Render Order)
        
        // 1. Sticky Note (Topmost Floating)
        if (stickyNote != null && stickyNote.mouseClicked(mouseX, mouseY, button)) { activeWidget = stickyNote; return true; }
        
        // 2. Source Panel (Floating)
        if (sourcePanel != null && sourcePanel.mouseClicked(mouseX, mouseY, button)) { activeWidget = sourcePanel; return true; }
        
        // 3. Toolbar
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY, button)) { activeWidget = toolbar; return true; }
        
        // 4. Selection Info Card
        if (selectionInfoCard != null && selectionInfoCard.mouseClicked(mouseX, mouseY, button)) { activeWidget = selectionInfoCard; return true; }
        
        // 5. Sequence Strip
        if (sequenceStrip != null && sequenceStrip.mouseClicked(mouseX, mouseY, button)) { activeWidget = sequenceStrip; return true; }
        
        // 6. Canvas (Background)
        if (canvasWidget != null && canvasWidget.mouseClicked(mouseX, mouseY, button)) { activeWidget = canvasWidget; return true; }
        
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Special handling for Drag & Drop
        if (isDragging) {

            // Check Drop Targets: Top -> Bottom
            // If mouse is over a component, we consider the drop "intercepted" by that layer.
            // Whether the component accepts the drop or not is up to its mouseReleased implementation.
            
            if (stickyNote != null && stickyNote.visible && stickyNote.isMouseOver(mouseX, mouseY)) {
                 stickyNote.mouseReleased(mouseX, mouseY, button);
            }
            else if (sourcePanel != null && sourcePanel.visible && sourcePanel.isMouseOver(mouseX, mouseY)) {
                sourcePanel.mouseReleased(mouseX, mouseY, button);
            }
            else if (toolbar != null && toolbar.visible && toolbar.isMouseOver(mouseX, mouseY)) {
                toolbar.mouseReleased(mouseX, mouseY, button);
            }
            else if (selectionInfoCard != null && selectionInfoCard.visible && selectionInfoCard.isMouseOver(mouseX, mouseY)) {
                selectionInfoCard.mouseReleased(mouseX, mouseY, button);
            }
            else if (sequenceStrip != null && sequenceStrip.visible && sequenceStrip.isMouseOver(mouseX, mouseY)) {
                sequenceStrip.mouseReleased(mouseX, mouseY, button);
            }
            else if (canvasWidget != null && canvasWidget.visible && canvasWidget.isMouseOver(mouseX, mouseY)) {
                canvasWidget.mouseReleased(mouseX, mouseY, button);
            }

            // If drag is still active (meaning no component consumed it and called endDrag),
            // we forcefully end it here (cancel the drag).
            if (isDragging) {
                endDrag();
            }
            
            activeWidget = null;
            return true;
        }

        // Standard Click Release
        boolean handled = false;
        if (activeWidget != null) {
            handled = activeWidget.mouseReleased(mouseX, mouseY, button);
            activeWidget = null;
        } else {
            // Fallback dispatch: Top -> Bottom
            if (stickyNote != null && stickyNote.mouseReleased(mouseX, mouseY, button)) handled = true;
            else if (sourcePanel != null && sourcePanel.mouseReleased(mouseX, mouseY, button)) handled = true;
            else if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY, button)) handled = true;
            else if (selectionInfoCard != null && selectionInfoCard.mouseReleased(mouseX, mouseY, button)) handled = true;
            else if (sequenceStrip != null && sequenceStrip.mouseReleased(mouseX, mouseY, button)) handled = true;
            else if (canvasWidget != null && canvasWidget.mouseReleased(mouseX, mouseY, button)) handled = true;
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
        // Scroll: Top -> Bottom
        if (stickyNote != null && stickyNote.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (sourcePanel != null && sourcePanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (toolbar != null && toolbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (selectionInfoCard != null && selectionInfoCard.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
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
    
    public WorkshopSession getSession() {
        return session;
    }
}
