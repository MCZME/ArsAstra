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
        // 图标索引 10
        this.manageBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.manuscript.manage"), 10, 0x4040A0, () -> {
            parentTab.toggleSelectionMode();
        });
        this.addChild(manageBtn);
        
        // 4. [Delete] 批量删除按钮 (初始隐藏)
        // 图标索引 11, 红色
        this.deleteBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.manuscript.delete_selected"), 11, 0xAA0000, () -> {
            parentTab.deleteSelected();
        });
        this.deleteBtn.visible = false;
        this.addChild(deleteBtn);
    }
    
    public void updateButtonsState() {
        boolean isManaging = parentTab.isSelectionMode();
        manageBtn.setColor(isManaging ? 0x804080 : 0x4040A0);
        deleteBtn.visible = isManaging;
        arrange();
    }

    @Override
    public void arrange() {
        if (searchWidget == null || filterWidget == null || manageBtn == null || deleteBtn == null) return;

        int padding = 2;
        int currentX = this.getX() + padding;

        // Left Group
        if (searchWidget.visible) {
            searchWidget.setX(currentX);
            searchWidget.setY(this.getY() + (this.height - searchWidget.getHeight()));
            currentX += searchWidget.getWidth() + padding;
        }

        if (filterWidget.visible) {
            filterWidget.setX(currentX);
            filterWidget.setY(this.getY() + (this.height - filterWidget.getHeight()));
        }
        
        // Right Group: Manage -> Delete (Delete is to the right of Manage)
        int rightX = this.getX() + this.width - padding;
        
        // 如果删除按钮可见，它占据最右边位置，管理按钮在它左边
        // 或者按照要求： "管理按钮... 就在这个按钮的右边 (指删除按钮)" -> 意思是 Manage | Delete
        
        if (deleteBtn.visible) {
            deleteBtn.setX(rightX - deleteBtn.getWidth());
            deleteBtn.setY(this.getY() + (this.height - deleteBtn.getHeight()));
            rightX -= (deleteBtn.getWidth() + padding);
        }
        
        if (manageBtn.visible) {
            manageBtn.setX(rightX - manageBtn.getWidth());
            manageBtn.setY(this.getY() + (this.height - manageBtn.getHeight()));
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
