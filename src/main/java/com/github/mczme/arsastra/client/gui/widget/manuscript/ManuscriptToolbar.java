package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;

import java.util.Collections;
import java.util.Set;

public class ManuscriptToolbar extends ToolbarWidget {
    private ToolbarSearchWidget searchWidget;
    private ManuscriptFilterWidget filterWidget;
    private final Runnable onFilterChanged;
    private String currentSearchQuery = "";

    public ManuscriptToolbar(int x, int y, int width, int height, Runnable onFilterChanged) {
        super(x, y, width, height);
        this.onFilterChanged = onFilterChanged;
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
    }

    @Override
    public void arrange() {
        // 确保组件已初始化
        if (searchWidget == null || filterWidget == null) return;

        int padding = 2;
        int currentX = this.getX() + padding;

        // Left Group: Search -> Filter
        if (searchWidget.visible) {
            searchWidget.setX(currentX);
            searchWidget.setY(this.getY() + (this.height - searchWidget.getHeight()));
            currentX += searchWidget.getWidth() + padding;
        }

        if (filterWidget.visible) {
            filterWidget.setX(currentX);
            filterWidget.setY(this.getY() + (this.height - filterWidget.getHeight()));
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
        // 每一帧都重新排列，以适应动态宽度的组件（如搜索框展开动画）
        arrange();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }
}
