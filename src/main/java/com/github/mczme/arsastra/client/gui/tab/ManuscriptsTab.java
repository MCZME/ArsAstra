package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptBookWidget;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptDetailOverlay;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptToolbar;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashSet;
import java.util.Set;

public class ManuscriptsTab implements JournalTab {
    private StarChartJournalScreen screen;
    private ManuscriptBookWidget bookWidget;
    private ManuscriptToolbar toolbar;
    private ManuscriptDetailOverlay detailOverlay;
    
    private boolean visible = false;
    
    private boolean isSelectionMode = false;
    private final Set<String> selectedManuscripts = new HashSet<>();
    
    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        ManuscriptManager.getInstance().loadAll();
        
        // 布局参数
        int toolbarY = y - 13;
        
        // 1. 书本分页组件
        this.bookWidget = new ManuscriptBookWidget(x, y, width, height, this);
        
        // 2. 工具栏组件 (由 Tab 内部管理，不添加到 Screen)
        this.toolbar = new ManuscriptToolbar(x + 15, toolbarY, 150, 22, this);
        this.toolbar.visible = false;
        
        // 3. 详情弹窗 (Overlay)
        this.detailOverlay = new ManuscriptDetailOverlay(screen.width, screen.height, this, () -> {
            // Close callback
        });
    }

    public StarChartJournalScreen getScreen() {
        return screen;
    }

    public ManuscriptToolbar getToolbar() {
        return toolbar;
    }

    public void refreshBook() {
        if (bookWidget != null) {
            bookWidget.refresh();
        }
    }

    public void onSelect(ClientManuscript manuscript) {
        if (isSelectionMode) {
            toggleSelection(manuscript.name());
        } else if (detailOverlay != null) {
            detailOverlay.show(manuscript);
        }
    }
    
    public void toggleSelectionMode() {
        this.isSelectionMode = !this.isSelectionMode;
        if (!isSelectionMode) {
            this.selectedManuscripts.clear();
        }
        // 通知 Toolbar 更新按钮状态 (例如显示/隐藏删除按钮)
        if (toolbar != null) {
            toolbar.updateButtonsState();
        }
    }
    
    public boolean isSelectionMode() {
        return isSelectionMode;
    }
    
    public void toggleSelection(String name) {
        if (selectedManuscripts.contains(name)) {
            selectedManuscripts.remove(name);
        } else {
            selectedManuscripts.add(name);
        }
    }
    
    public boolean isSelected(String name) {
        return selectedManuscripts.contains(name);
    }
    
    public void deleteSelected() {
        if (selectedManuscripts.isEmpty()) return;
        
        for (String name : selectedManuscripts) {
            ManuscriptManager.getInstance().deleteManuscript(name);
        }
        selectedManuscripts.clear();
        refreshBook();
        // 保持选择模式还是退出？通常保持方便继续操作，或者退出。这里保持。
    }

    public void loadSelected() {
        if (selectedManuscripts.isEmpty()) return;

        java.util.List<com.github.mczme.arsastra.core.starchart.engine.AlchemyInput> combinedSequence = new java.util.ArrayList<>();
        
        // 按名称排序以保证一定程度的确定性，或者维持选中顺序
        // 这里按本地存储的顺序查找选中的手稿
        var allManuscripts = com.github.mczme.arsastra.core.manuscript.ManuscriptManager.getInstance().getManuscripts();
        
        for (var manuscript : allManuscripts) {
            if (selectedManuscripts.contains(manuscript.name())) {
                combinedSequence.addAll(manuscript.inputs());
            }
        }

        if (screen.getTab(1) instanceof WorkshopTab workshopTab) {
            workshopTab.getSession().loadSequence(combinedSequence);
            screen.switchTab(1);
            toggleSelectionMode(); // 加载后退出管理模式
        }
    }

    @Override
    public void tick() {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        boolean overlayActive = detailOverlay != null && detailOverlay.visible;
        
        // 当覆盖层激活时，向底层组件传递无效的鼠标坐标，防止悬停效果和 Tooltip 显示
        int bgMouseX = overlayActive ? -1 : mouseX;
        int bgMouseY = overlayActive ? -1 : mouseY;

        if (bookWidget != null) {
            bookWidget.render(guiGraphics, bgMouseX, bgMouseY, partialTick);
        }
        
        if (toolbar != null) {
            toolbar.render(guiGraphics, bgMouseX, bgMouseY, partialTick);
        }
        
        if (overlayActive) {
            detailOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (toolbar != null) toolbar.visible = visible;
        if (!visible && detailOverlay != null) detailOverlay.hide();
        // 切换 Tab 时退出选择模式
        if (!visible && isSelectionMode) toggleSelectionMode();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { 
        if (!visible) return false;
        
        // 1. Overlay (最高优先级)
        if (detailOverlay != null && detailOverlay.visible) {
            if (detailOverlay.mouseClicked(mouseX, mouseY, button)) return true;
        }
        
        // 2. Toolbar (包含弹出窗口，可能遮挡书本)
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // 3. Book Widget (最低优先级)
        if (bookWidget != null && bookWidget.mouseClicked(mouseX, mouseY, button)) return true;
        
        return false; 
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { 
        if (!visible) return false;
        if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY, button)) return true;
        if (bookWidget != null && bookWidget.mouseReleased(mouseX, mouseY, button)) return true;
        return false; 
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { 
        if (!visible) return false;
        if (toolbar != null && toolbar.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (bookWidget != null && bookWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return false; 
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { 
        if (!visible) return false;
        if (toolbar != null && toolbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return false; 
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) { 
        if (!visible) return false;
        if (toolbar != null && toolbar.charTyped(codePoint, modifiers)) return true;
        return false; 
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { 
        if (!visible) return false;
        if (toolbar != null && toolbar.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (bookWidget != null && bookWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false; 
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
}
