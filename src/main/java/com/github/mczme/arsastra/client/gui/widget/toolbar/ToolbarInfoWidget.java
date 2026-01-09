package com.github.mczme.arsastra.client.gui.widget.toolbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ToolbarInfoWidget extends ToolbarExpandableWidget {

    public ToolbarInfoWidget(int x, int y) {
        // 图标索引 9 (信息), 靛蓝色 (0x406080), 弹出框大小 160x100
        super(x, y, 160, 100, 9, 0x406080);
        this.setExpandDirection(ExpandDirection.LEFT);
        updatePopupLayout();
    }

    @Override
    protected void updatePopupLayout() {
        // 纯展示组件，内部无交互控件需要布局
    }

    @Override
    protected boolean mouseClickedInPopup(double mouseX, double mouseY, int button) {
        // 纯展示，不消耗点击（或者返回 false 让基类处理为消耗但不动作）
        // 返回 false 会导致基类返回 true (消耗点击)，这符合预期（点击面板背景不关闭）
        return false;
    }

    @Override
    protected void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int bgX, int bgY) {
        // 标题
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.workshop.info_panel.title"), bgX + 5, bgY + 5, 0xAAAAAA, false);
        
        // 分割线 (模拟)
        guiGraphics.fill(bgX + 5, bgY + 16, bgX + popupWidth - 5, bgY + 17, 0xFF604030);

        // 内容
        // TODO: 从 ViewModel 获取真实数据
        Component content = Component.literal("Stability: 100%\nComplexity: Low\n\nPath data and other stats will be shown here.");
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, content, bgX + 5, bgY + 22, popupWidth - 10, 0xCCCCCC);
    }
}
