package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptBookWidget;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptDetailOverlay;
import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptToolbar;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import net.minecraft.client.gui.GuiGraphics;

public class ManuscriptsTab implements JournalTab {
    private StarChartJournalScreen screen;
    private ManuscriptBookWidget bookWidget;
    private ManuscriptToolbar toolbar;
    private ManuscriptDetailOverlay detailOverlay;
    
    private boolean visible = false;
    
    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        ManuscriptManager.getInstance().loadAll();
        
        // 布局参数
        int toolbarY = y - 13;
        
        // 1. 书本分页组件
        this.bookWidget = new ManuscriptBookWidget(x, y, width, height, this);
        
        // 2. 工具栏组件
        this.toolbar = new ManuscriptToolbar(x + 15, toolbarY, 150, 22, () -> {
            if (this.bookWidget != null) this.bookWidget.refresh();
        });
        this.toolbar.visible = false;
        screen.addTabWidget(this.toolbar);
        
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
        if (detailOverlay != null) {
            detailOverlay.show(manuscript);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        if (bookWidget != null) {
            bookWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (detailOverlay != null && detailOverlay.visible) {
            detailOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (toolbar != null) toolbar.visible = visible;
        if (!visible && detailOverlay != null) detailOverlay.hide();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) { 
        if (!visible) return false;
        
        if (detailOverlay != null && detailOverlay.visible) {
            if (detailOverlay.mouseClicked(mouseX, mouseY, button)) return true;
        }
        
        if (bookWidget != null && bookWidget.mouseClicked(mouseX, mouseY, button)) return true;
        return false; 
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { 
        if (!visible) return false;
        // Should overlay handle release? Usually abstract widget handles it if needed.
        if (bookWidget != null && bookWidget.mouseReleased(mouseX, mouseY, button)) return true;
        return false; 
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { 
        if (!visible) return false;
        if (bookWidget != null && bookWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return false; 
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { 
        if (!visible) return false;
        return false; 
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) { 
        if (!visible) return false;
        return false; 
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { 
        if (!visible) return false;
        if (bookWidget != null && bookWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false; 
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
}
