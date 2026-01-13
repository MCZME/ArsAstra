package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import java.util.Map;

public class StickyNoteWidget extends FloatingWidget {
    private final WorkshopSession session;

    public StickyNoteWidget(int x, int y, WorkshopSession session) {
        super(x, y, 130, 120, Component.translatable("gui.ars_astra.workshop.note"));
        this.session = session;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 背景 (羊皮纸色，模拟便签纸)
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Palette.PARCHMENT_BG);
        
        // 2. 手绘风格边框
        renderHandDrawnBorder(guiGraphics);
        
        // 3. 顶部装饰
        guiGraphics.fill(getX() + 10, getY() - 2, getX() + getWidth() - 10, getY() + 4, Palette.INK_LIGHT);

        // 4. 标题
        int titleHeight = 14;
        guiGraphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 6, getY() + 4, Palette.INK, false);
        guiGraphics.fill(getX() + 5, getY() + titleHeight, getX() + 40, getY() + titleHeight + 1, Palette.INK);

        // 5. 内容：推演结果
        DeductionResult result = session.getDeductionResult();
        if (result != null) {
            renderResultInfo(guiGraphics, result, getY() + titleHeight + 8);
        } else {
            renderEmptyState(guiGraphics, getY() + titleHeight + 15);
        }
    }

    private void renderHandDrawnBorder(GuiGraphics guiGraphics) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        
        guiGraphics.renderOutline(x, y, w, h, Palette.INK);
        guiGraphics.renderOutline(x + 1, y + 1, w - 2, h - 2, Palette.INK_LIGHT);
    }

    private void renderResultInfo(GuiGraphics guiGraphics, DeductionResult result, int startY) {
        int padding = 8;
        int currentY = startY;

        // 1. 稳定性 (Stability)
        String stabilityStr = String.format("%.0f%%", result.finalStability() * 100);
        // 颜色：高绿，中黄，低红 (这里使用硬编码颜色，或者定义在 Palette 中)
        int color = result.finalStability() > 0.8f ? 0xFF208020 : (result.finalStability() > 0.5f ? 0xFFA0A020 : 0xFF802020);
        
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.workshop.stability"), getX() + padding, currentY, Palette.INK, false);
        guiGraphics.drawString(Minecraft.getInstance().font, stabilityStr, getX() + getWidth() - padding - Minecraft.getInstance().font.width(stabilityStr), currentY, color, false);
        
        currentY += 14;
        
        // 分割线
        guiGraphics.fill(getX() + padding, currentY, getX() + getWidth() - padding, currentY + 1, Palette.INK_LIGHT);
        currentY += 8;
        
        // 2. 预测效果 (Effects)
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.workshop.effects"), getX() + padding, currentY, Palette.INK, false);
        currentY += 12;
        
        if (result.predictedEffects().isEmpty()) {
             guiGraphics.drawString(Minecraft.getInstance().font, "-", getX() + padding + 5, currentY, Palette.INK_LIGHT, false);
        } else {
            // 获取知识数据
            StarChart starChart = StarChartManager.getInstance().getStarChart(session.getStarChartId()).orElse(null);
            PlayerKnowledge knowledge = session.getKnowledge();

            int maxEffects = 3; // 最多显示3个，避免溢出
            int count = 0;
            for (Map.Entry<EffectField, PotionData> entry : result.predictedEffects().entrySet()) {
                if (count >= maxEffects) {
                    guiGraphics.drawString(Minecraft.getInstance().font, "...", getX() + padding + 5, currentY, Palette.INK_LIGHT, false);
                    break;
                }
                
                boolean unlocked = false;
                if (starChart != null && knowledge != null) {
                    int index = starChart.fields().indexOf(entry.getKey());
                    if (index >= 0 && knowledge.hasUnlockedField(session.getStarChartId(), index)) {
                        unlocked = true;
                    }
                }

                if (!unlocked) {
                    guiGraphics.drawString(Minecraft.getInstance().font, "???", getX() + padding + 5, currentY, Palette.INK_LIGHT, false);
                    currentY += 10;
                    count++;
                    continue;
                }
                
                MobEffect effect = entry.getKey().getEffect();
                if (effect != null) {
                    Component name = effect.getDisplayName();
                    String lvl = "Lv." + entry.getValue().level();
                    
                    // 名称
                    guiGraphics.drawString(Minecraft.getInstance().font, name, getX() + padding + 5, currentY, Palette.INK, false);
                    // 等级 (靠右)
                    guiGraphics.drawString(Minecraft.getInstance().font, lvl, getX() + getWidth() - padding - Minecraft.getInstance().font.width(lvl), currentY, Palette.INK_LIGHT, false);
                    
                    currentY += 10;
                    count++;
                }
            }
        }
    }

    private void renderEmptyState(GuiGraphics guiGraphics, int startY) {
        Component text = Component.translatable("gui.ars_astra.workshop.no_result");
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, text, getX() + 10, startY, getWidth() - 20, Palette.INK_LIGHT);
    }
}