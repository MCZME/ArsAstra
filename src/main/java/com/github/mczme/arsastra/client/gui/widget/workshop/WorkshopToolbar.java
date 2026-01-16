package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ManuscriptSaveWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarClearWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarFilterWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSettingsWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WorkshopToolbar extends ToolbarWidget {
    private final WorkshopActionHandler handler;
    private final WorkshopSession session;
    
    private ToolbarSearchWidget searchWidget;
    private ToolbarFilterWidget filterWidget;
    private ToolbarClearWidget clearBtn;
    private ManuscriptSaveWidget saveWidget;
    private ToolbarTabButton infoBtn;
    private ToolbarSettingsWidget settingsBtn;
    private String currentSearchQuery = "";

    public WorkshopToolbar(int x, int y, int width, int height, WorkshopSession session, WorkshopActionHandler handler) {
        super(x, y, width, height);
        this.handler = handler;
        this.session = session;
        initButtons();
    }
    
    private void initButtons() {
        // 1. [Clear] 清空组件
        this.clearBtn = new ToolbarClearWidget(0, 0, () -> {
            if (handler != null) handler.onClearRequest();
        });
        this.addChild(clearBtn);

        // 2. [Save] 保存组件 (ManuscriptSaveWidget)
        this.saveWidget = new ManuscriptSaveWidget(0, 0, (name, iconIndex) -> {
            List<AlchemyInput> inputs = session.getInputs();
            if (inputs.isEmpty()) return;
            
            // Icon index 转换为字符串ID，这里简单用 index 字符串，实际可以映射到资源名
            String iconId = String.valueOf(iconIndex);

            List<String> outcome = new java.util.ArrayList<>();
            if (session.getDeductionResult() != null) {
                session.getDeductionResult().predictedEffects().forEach((field, data) -> {
                    net.minecraft.world.effect.MobEffect effect = field.getEffect();
                    if (effect != null) {
                        StringBuilder sb = new StringBuilder(effect.getDisplayName().getString());
                        if (data.level() > 0) {
                            sb.append(" ").append(data.level() + 1);
                        }
                        
                        int totalSeconds = data.duration() / 20;
                        int mins = totalSeconds / 60;
                        int secs = totalSeconds % 60;
                        sb.append(String.format(" (%d:%02d)", mins, secs));
                        
                        outcome.add(sb.toString());
                    }
                });
            }
            
            ClientManuscript manuscript = new ClientManuscript(
                name,
                iconId,
                System.currentTimeMillis(),
                session.getStarChartId(),
                inputs,
                outcome
            );
            ManuscriptManager.getInstance().saveManuscript(manuscript);
        });
        this.addChild(saveWidget);

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

        // 5. [Info] 信息按钮: 靛蓝色 (0x406080), 图标索引 9 (信息)
        this.infoBtn = new ToolbarTabButton(0, 0, 20, 22, Component.translatable("gui.ars_astra.workshop.info"), 9, 0x406080, () -> {
            if (handler != null) handler.onInfoToggle();
        });
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
            clearBtn == null || saveWidget == null || infoBtn == null) {
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

        if (saveWidget.visible) {
            rightX -= saveWidget.getWidth();
            saveWidget.setX(rightX);
            saveWidget.setY(this.getY() + (this.height - saveWidget.getHeight()));
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
}