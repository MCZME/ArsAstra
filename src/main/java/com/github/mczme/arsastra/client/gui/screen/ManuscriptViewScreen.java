package com.github.mczme.arsastra.client.gui.screen;

import com.github.mczme.arsastra.client.gui.widget.manuscript.ManuscriptDetailOverlay;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ManuscriptViewScreen extends Screen {
    private final ClientManuscript manuscript;
    private ManuscriptDetailOverlay overlay;

    public ManuscriptViewScreen(ClientManuscript manuscript) {
        super(Component.literal(manuscript.name()));
        this.manuscript = manuscript;
    }

    @Override
    protected void init() {
        // 创建只读模式的 Overlay (isReadOnly = true, parentTab = null)
        this.overlay = new ManuscriptDetailOverlay(this.width, this.height, null, true, this::onClose);
        this.addRenderableWidget(this.overlay);
        this.overlay.show(manuscript);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 先渲染背景（全屏变暗）
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
