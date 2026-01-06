package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import net.minecraft.client.gui.GuiGraphics;

public interface JournalTab {
    void init(StarChartJournalScreen screen, int x, int y, int width, int height);
    void tick();
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
    void setVisible(boolean visible);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    boolean mouseReleased(double mouseX, double mouseY, int button);
    boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY);
    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);
    boolean charTyped(char codePoint, int modifiers);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean keyReleased(int keyCode, int scanCode, int modifiers);
}
