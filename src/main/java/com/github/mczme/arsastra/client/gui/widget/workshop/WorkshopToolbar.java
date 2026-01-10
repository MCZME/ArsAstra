package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarClearWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarFilterWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarInfoWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSettingsWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import net.minecraft.network.chat.Component;

public class WorkshopToolbar extends ToolbarWidget {
    private final WorkshopActionHandler handler;
    
    private ToolbarSearchWidget searchWidget;
    private ToolbarFilterWidget filterWidget;
    private ToolbarClearWidget clearBtn;
    private ToolbarTabButton saveBtn;
    private ToolbarInfoWidget infoBtn;
    private ToolbarSettingsWidget settingsBtn;
    private String currentSearchQuery = "";

    public WorkshopToolbar(int x, int y, int width, int height, WorkshopSession session, WorkshopActionHandler handler) {
        super(x, y, width, height);
        this.handler = handler;
        initButtons();
    }
    
    private void initButtons() {
        // 1. [Clear] 清空组件
        this.clearBtn = new ToolbarClearWidget(0, 0, () -> {
            if (handler != null) handler.onClearRequest();
        });
        this.addChild(clearBtn);

        // 2. [Save] 保存按钮: 灰色 (0x888888), 图标索引 9 (墨水瓶与羽毛笔)
        this.saveBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.workshop.save"), 8, 0x888888, this::onSave);
        saveBtn.active = false;
        this.addChild(saveBtn);

        // 3. [Search] 搜索组件
        this.searchWidget = new ToolbarSearchWidget(0, 0, (query) -> {
            this.currentSearchQuery = query.toLowerCase();
            if (handler != null) handler.onFilterChanged();
        });
        this.addChild(this.searchWidget);

        // 4. [Filter] 筛选组件
        this.filterWidget = new ToolbarFilterWidget(0, 0, () -> {
            if (handler != null) handler.onFilterChanged();
        });
        this.addChild(this.filterWidget);

        // 5. [Info] 信息组件
        this.infoBtn = new ToolbarInfoWidget(0, 0);
        this.addChild(infoBtn);

        // 6. [Settings] 设置组件
        this.settingsBtn = new ToolbarSettingsWidget(0, 0, (type) -> {
            if (handler != null) handler.onChartTypeChanged(type);
        });
        this.addChild(settingsBtn);
    }
    
    @Override
    public void arrange() {
        // 确保所有组件都已初始化
        if (settingsBtn == null || searchWidget == null || filterWidget == null || 
            clearBtn == null || saveBtn == null || infoBtn == null) {
            return;
        }

        int padding = 2;
        int currentX = this.getX() + padding;

        // Left Group: Settings -> Search -> Filter
        if (settingsBtn.visible) {
            settingsBtn.setX(currentX);
            settingsBtn.setY(this.getY() + (this.height - settingsBtn.getHeight()));
            currentX += settingsBtn.getWidth() + padding;
        }

        if (searchWidget.visible) {
            searchWidget.setX(currentX);
            searchWidget.setY(this.getY() + (this.height - searchWidget.getHeight()));
            currentX += searchWidget.getWidth() + padding;
        }

        if (filterWidget.visible) {
            filterWidget.setX(currentX);
            filterWidget.setY(this.getY() + (this.height - filterWidget.getHeight()));
        }

        // Right Group: Clear -> Save -> Info (Aligned to Right)
        int rightX = this.getX() + this.width - padding;

        if (infoBtn.visible) {
            rightX -= infoBtn.getWidth();
            infoBtn.setX(rightX);
            infoBtn.setY(this.getY() + (this.height - infoBtn.getHeight()));
            rightX -= padding;
        }

        if (saveBtn.visible) {
            rightX -= saveBtn.getWidth();
            saveBtn.setX(rightX);
            saveBtn.setY(this.getY() + (this.height - saveBtn.getHeight()));
            rightX -= padding;
        }

        if (clearBtn.visible) {
            rightX -= clearBtn.getWidth();
            clearBtn.setX(rightX);
            clearBtn.setY(this.getY() + (this.height - clearBtn.getHeight()));
        }
    }

    public String getSearchQuery() {
        return currentSearchQuery;
    }

    public String getElementFilter() {
        return filterWidget != null ? filterWidget.getElementFilter() : "";
    }

    public String getTagFilter() {
        return filterWidget != null ? filterWidget.getTagFilter() : "";
    }

    private void onSave() {
        if (handler != null) handler.onSaveRequest();
    }
}
