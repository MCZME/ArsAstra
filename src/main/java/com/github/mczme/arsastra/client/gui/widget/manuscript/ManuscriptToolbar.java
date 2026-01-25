package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.tab.ManuscriptsTab;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ManuscriptFilterWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Set;

public class ManuscriptToolbar extends ToolbarWidget {
    private final ManuscriptsTab parentTab;
    private final Runnable onFilterChanged;
    
    private ToolbarSearchWidget searchWidget;
    private ManuscriptFilterWidget filterWidget;
    
    private ToolbarTabButton manageBtn;
    private ToolbarTabButton deleteBtn;
    private ToolbarTabButton batchLoadBtn;
    
    private String currentSearchQuery = "";

    public ManuscriptToolbar(int x, int y, int width, int height, ManuscriptsTab parentTab) {
        super(x, y, width, height);
        this.parentTab = parentTab;
        this.onFilterChanged = () -> parentTab.refreshBook();
        initButtons();
    }

    private void initButtons() {
        // 1. [Search] 搜索组件
        this.searchWidget = new ToolbarSearchWidget(0, 0, (query) -> {
            this.currentSearchQuery = query.toLowerCase();
            if (onFilterChanged != null) onFilterChanged.run();
        });
        this.addChild(searchWidget);

        // 2. [Filter] 筛选组件
        this.filterWidget = new ManuscriptFilterWidget(0, 0, () -> {
            if (onFilterChanged != null) onFilterChanged.run();
        });
        this.addChild(filterWidget);
        
        // 3. [Manage] 管理按钮
        // 图标索引 13
        this.manageBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.manuscript.manage"), 13, 0x6080D0, () -> {
            parentTab.toggleSelectionMode();
        });
        this.addChild(manageBtn);

        // 4. [Batch Load] 批量加载按钮
        // 图标索引 12, 绿色
        this.batchLoadBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.manuscript.batch_load"), 12, 0x1E7636, () -> {
            parentTab.loadSelected();
        });
        this.batchLoadBtn.visible = false;
        this.addChild(batchLoadBtn);
        
        // 5. [Delete] 批量删除按钮 (初始隐藏)
        // 图标索引 14, 红色
        this.deleteBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.manuscript.delete_selected"), 14, 0xD06060, () -> {
            parentTab.deleteSelected();
        });
        this.deleteBtn.visible = false;
        this.addChild(deleteBtn);
    }
    
    public void updateButtonsState() {
        boolean isManaging = parentTab.isSelectionMode();
        manageBtn.setColor(isManaging ? 0x9060D0 : 0x6080D0);
        batchLoadBtn.visible = isManaging;
        deleteBtn.visible = isManaging;
        arrange();
    }

    @Override
    public void arrange() {
        if (searchWidget == null || filterWidget == null || manageBtn == null || deleteBtn == null || batchLoadBtn == null) return;

        int padding = 2;
        int currentX = this.getX() + padding;

        // Sequence: Search -> Filter -> Manage -> BatchLoad -> Delete
        if (searchWidget.visible) {
            searchWidget.setX(currentX);
            searchWidget.setY(this.getY() + (this.height - searchWidget.getHeight()));
            currentX += searchWidget.getWidth() + padding;
        }

        if (filterWidget.visible) {
            filterWidget.setX(currentX);
            filterWidget.setY(this.getY() + (this.height - filterWidget.getHeight()));
            currentX += filterWidget.getWidth() + padding;
        }

        if (manageBtn.visible) {
            manageBtn.setX(currentX);
            manageBtn.setY(this.getY() + (this.height - manageBtn.getHeight()));
            currentX += manageBtn.getWidth() + padding;
        }

        if (batchLoadBtn.visible) {
            batchLoadBtn.setX(currentX);
            batchLoadBtn.setY(this.getY() + (this.height - batchLoadBtn.getHeight()));
            currentX += batchLoadBtn.getWidth() + padding;
        }

        if (deleteBtn.visible) {
            deleteBtn.setX(currentX);
            deleteBtn.setY(this.getY() + (this.height - deleteBtn.getHeight()));
        }
    }

    public String getSearchQuery() {
        return currentSearchQuery;
    }

    public Set<Integer> getFilterIcons() {
        return filterWidget != null ? filterWidget.getSelectedIcons() : Collections.emptySet();
    }

    public String getFilterItem() {
        return filterWidget != null ? filterWidget.getItemFilter() : "";
    }

    public String getFilterEffect() {
        return filterWidget != null ? filterWidget.getEffectFilter() : "";
    }
    
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        arrange();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
