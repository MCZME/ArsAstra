package com.github.mczme.arsastra.client.gui.widget.toolbar;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ToolbarSettingsWidget extends ToolbarExpandableWidget {
    private final WorkshopSession session;
    
    private SettingSlider capacitySlider;
    private SettingSlider decaySlider;
    private Button baseChartButton; // 恢复星图切换按钮

    public ToolbarSettingsWidget(int x, int y, WorkshopSession session) {
        // 宽度 140, 高度 100 (容纳 2 个滑块 + 1 个按钮 + 间距)
        super(x, y, 140, 100, 10, 0x606060);
        this.session = session;
        
        initWidgets();
        updatePopupLayout();
    }
    
    private void initWidgets() {
        // 1. 容量滑块 (1 - 16)
        double initialCapacity = (session.getMaxInput() - 1) / 15.0;
        this.capacitySlider = new SettingSlider(0, 0, 120, 20, initialCapacity) {
            @Override
            protected void updateMessage() {
                int val = 1 + (int)(this.value * 15);
                this.setMessage(Component.translatable("gui.ars_astra.workshop.settings.capacity", val));
            }

            @Override
            protected void applyValue() {
                int val = 1 + (int)(this.value * 15);
                session.setSimulationParameters(val, session.getDecayFactor());
            }
        };

        // 2. 容器系数滑块 (0.5 - 2.0)
        double initialDecay = (session.getDecayFactor() - 0.5) / 1.5;
        this.decaySlider = new SettingSlider(0, 0, 120, 20, initialDecay) {
            @Override
            protected void updateMessage() {
                float val = 0.5f + (float)(this.value * 1.5f);
                String valStr = String.format("%.1f", val);
                // 修正文案：Decay -> Container Factor
                this.setMessage(Component.translatable("gui.ars_astra.workshop.settings.decay", valStr));
            }

            @Override
            protected void applyValue() {
                float val = 0.5f + (float)(this.value * 1.5f);
                session.setSimulationParameters(session.getMaxInput(), val);
            }
        };
        
        // 3. 基础星图切换按钮 (临时功能，用于重置)
        this.baseChartButton = Button.builder(Component.translatable("gui.ars_astra.workshop.settings.base_chart"), (btn) -> {
            session.setStarChartId(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_astra", "base_chart"));
            setExpanded(false);
        }).bounds(0, 0, 120, 20).build();
    }

    @Override
    protected void onCollapse() {
        this.capacitySlider.setFocused(false);
        this.decaySlider.setFocused(false);
        this.baseChartButton.setFocused(false);
    }
    
    @Override
    protected void onExpand() {
        // 每次展开时同步当前 Session 的值（防止外部修改后不同步）
        double capVal = (session.getMaxInput() - 1) / 15.0;
        this.capacitySlider.setSliderValue(capVal);
        
        double decayVal = (session.getDecayFactor() - 0.5) / 1.5;
        this.decaySlider.setSliderValue(decayVal);
    }

    @Override
    protected void updatePopupLayout() {
        int bgX = getPopupX();
        int bgY = this.getY() + 22;
        
        int padding = 10;
        int yOffset = bgY + padding;
        
        if (capacitySlider != null) {
            capacitySlider.setX(bgX + padding);
            capacitySlider.setY(yOffset);
            yOffset += 24;
        }
        
        if (decaySlider != null) {
            decaySlider.setX(bgX + padding);
            decaySlider.setY(yOffset);
            yOffset += 24;
        }
        
        if (baseChartButton != null) {
            baseChartButton.setX(bgX + padding);
            baseChartButton.setY(yOffset);
        }
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        capacitySlider.render(guiGraphics, mouseX, mouseY, partialTick);
        decaySlider.render(guiGraphics, mouseX, mouseY, partialTick);
        baseChartButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (capacitySlider.mouseClicked(mouseX, mouseY, button)) handled = true;
        else if (decaySlider.mouseClicked(mouseX, mouseY, button)) handled = true;
        else if (baseChartButton.mouseClicked(mouseX, mouseY, button)) handled = true;
        
        // 关键修复：只要是点击在 Popup 范围内（父类已经判断过 inPopup=true 才会调用此方法），
        // 即使没有点中任何按钮，也返回 true 以消费事件，防止穿透到底层。
        return true; 
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (expanded) {
            if (capacitySlider.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
            if (decaySlider.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            if (capacitySlider.mouseReleased(mouseX, mouseY, button)) return true;
            if (decaySlider.mouseReleased(mouseX, mouseY, button)) return true;
            if (baseChartButton.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 自定义滑块类，用于暴露 setValue 方法
     */
    private abstract static class SettingSlider extends AbstractSliderButton {
        public SettingSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Component.empty(), value);
        }

        public void setSliderValue(double newValue) {
            this.value = Mth.clamp(newValue, 0.0, 1.0);
            this.updateMessage();
        }
    }
}