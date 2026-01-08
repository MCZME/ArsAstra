package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopViewModel;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class WorkshopToolbar extends ToolbarWidget {
    private final WorkshopViewModel viewModel;

    public WorkshopToolbar(int x, int y, int width, int height, WorkshopViewModel viewModel) {
        super(x, y, width, height);
        this.viewModel = viewModel;
        initButtons();
    }

    private void initButtons() {
        // [Clear] 按钮: 红色 (0xFF5555), 图标索引暂定 0
        ToolbarTabButton clearBtn = new ToolbarTabButton(0, 0, 20, 20, Component.translatable("gui.ars_astra.workshop.clear"), 0, 0xFF5555, this::onClear);
        clearBtn.setTooltip(Tooltip.create(Component.translatable("gui.ars_astra.workshop.clear")));
        this.addChild(clearBtn);

        // [Save] 按钮: 蓝色 (0x5555FF), 图标索引暂定 1
        ToolbarTabButton saveBtn = new ToolbarTabButton(0, 0, 20, 20, Component.translatable("gui.ars_astra.workshop.save"), 1, 0x5555FF, this::onSave);
        saveBtn.setTooltip(Tooltip.create(Component.translatable("gui.ars_astra.workshop.save")));
        this.addChild(saveBtn);
    }

    private void onClear() {
        viewModel.clear();
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void onSave() {
        // TODO: Implement save logic
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
