package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.tab.ManuscriptsTab;
import com.github.mczme.arsastra.client.gui.tab.WorkshopTab;
import com.github.mczme.arsastra.client.gui.util.PathRenderer;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.*;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionService;
import com.github.mczme.arsastra.core.starchart.engine.service.DeductionServiceImpl;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ManuscriptDetailOverlay extends AbstractWidget {
    private static final ResourceLocation MANUSCRIPT_PAPER = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_paper.png");
    
    private final ManuscriptsTab parentTab;
    private ClientManuscript manuscript;
    private final Runnable onClose;
    private final int screenWidth;
    private final int screenHeight;
    
    private TextButton closeButton;
    private TextButton loadButton;
    private TextButton deleteButton;
    
    private boolean confirmDelete = false;
    
    private final DeductionService deductionService = new DeductionServiceImpl();
    private DeductionResult deductionResult; 
    
    private final List<ManuscriptStepWidget> stepWidgets = new ArrayList<>();
    
    private boolean layoutDirty = true;
    
    // Cached Layout Data
    private int paperX, paperY;
    private int previewH, previewX, previewY, previewW;
    private int resultStartY, resultHeight;

    // 纸张尺寸
    private static final int PAPER_WIDTH = 104;
    private static final int PAPER_HEIGHT = 162;
    
    public ManuscriptDetailOverlay(int screenWidth, int screenHeight, ManuscriptsTab parentTab, Runnable onClose) {
        super(0, 0, screenWidth, screenHeight, Component.empty());
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.parentTab = parentTab;
        this.onClose = onClose;
        this.visible = false;
        
        initButtons();
    }
    
    private void initButtons() {
        int centerX = (screenWidth - PAPER_WIDTH) / 2;
        int centerY = (screenHeight - PAPER_HEIGHT) / 2;
        
        this.closeButton = new TextButton(centerX + PAPER_WIDTH - 15, centerY + 8, 10, 10, "x", 0xFF000000, b -> hide());
            
        // 加载按钮 (底部居右文字)
        this.loadButton = new TextButton(centerX + PAPER_WIDTH - 40, centerY + PAPER_HEIGHT - 25, 30, 15, Component.translatable("gui.ars_astra.load").getString(), 0xFF1E7636, b -> {
            if (manuscript != null) {
                if (parentTab.getScreen().getTab(1) instanceof WorkshopTab workshopTab) {
                    workshopTab.getSession().setStarChartId(manuscript.chart());
                    workshopTab.getSession().loadSequence(manuscript.inputs());
                    parentTab.getScreen().switchTab(1);
                    hide();
                }
            }
        });

        this.deleteButton = new TextButton(centerX + 10, centerY + PAPER_HEIGHT - 25, 30, 15, Component.translatable("gui.ars_astra.delete").getString(), 0xFF8B2500, b -> {
            if (!confirmDelete) {
                confirmDelete = true;
                b.setMessage(Component.translatable("gui.ars_astra.confirm"));
            } else {
                if (manuscript != null) {
                    ManuscriptManager.getInstance().deleteManuscript(manuscript.name());
                    parentTab.refreshBook();
                    hide();
                }
            }
        });
    }
    
    public void show(ClientManuscript manuscript) {
        this.manuscript = manuscript;
        this.visible = true;
        this.confirmDelete = false;
        this.deleteButton.setMessage(Component.translatable("gui.ars_astra.delete"));
        
        this.stepWidgets.clear();
        for (int i = 0; i < manuscript.inputs().size(); i++) {
            this.stepWidgets.add(new ManuscriptStepWidget(manuscript.inputs().get(i), i));
        }
        
        StarChartManager.getInstance().getStarChart(manuscript.chart())
            .ifPresent(chart -> {
                this.deductionResult = deductionService.deduce(chart, manuscript.inputs());
            });
            
        this.layoutDirty = true;
    }
    
    public void hide() {
        this.visible = false;
        if (onClose != null) {
            onClose.run();
        }
    }

    private void recalculateLayout() {
        this.paperX = (screenWidth - PAPER_WIDTH) / 2;
        this.paperY = (screenHeight - PAPER_HEIGHT) / 2;
        
        int inputCount = manuscript.inputs().size();
        
        // 1. Calculate Preview Area
        this.previewH = inputCount > 42 ? 20 : 27;
        this.previewX = paperX + 12;
        this.previewY = paperY + 28;
        this.previewW = PAPER_WIDTH - 24;
        
        // 2. Calculate Result Area
        this.resultHeight = 0;
        this.resultStartY = paperY + 137;
        Map<EffectField, PotionData> effects = deductionResult != null ? deductionResult.predictedEffects() : Map.of();
        if (!effects.isEmpty()) {
            int resultRows = (effects.size() + 2) / 3;
            this.resultHeight = Math.max(20, resultRows * 14); 
            this.resultStartY = paperY + 137 - resultHeight;
        }
        
        // 3. Calculate Ingredients Layout
        if (inputCount > 0) {
            int contentStartY = previewY + previewH + 4;
            int contentEndY = resultStartY - 2;
            int availableHeight = contentEndY - contentStartY;
            
            int maxCols;
            float itemScale;
            
            if (inputCount <= 5) {
                maxCols = inputCount;
                itemScale = 0.9f;
            } else if (inputCount <= 12) {
                maxCols = 4;
                itemScale = 0.8f;
            } else if (inputCount <= 24) {
                maxCols = 5;
                itemScale = 0.65f;
            } else if (inputCount <= 42) {
                maxCols = 6;
                itemScale = 0.55f;
            } else {
                maxCols = 8;
                itemScale = 0.45f;
            }
            
            int colSpacing = (int)(16 * itemScale + 2); 
            int rowSpacing = (int)(16 * itemScale + 2);
            
            int actualRows = (inputCount + maxCols - 1) / maxCols;
            int totalBlockWidth = (Math.min(inputCount, maxCols) - 1) * colSpacing;
            int totalBlockHeight = (actualRows - 1) * rowSpacing;
            
            int centerY = contentStartY + availableHeight / 2;
            int rawStartY = centerY - totalBlockHeight / 2;
            final int startY = Math.max(rawStartY, contentStartY);
            
            int startXCenter = paperX + (PAPER_WIDTH - totalBlockWidth) / 2;
            
            final int finalMaxCols = maxCols;
            for (int i = 0; i < inputCount; i++) {
                if (i < stepWidgets.size()) {
                    int r = i / finalMaxCols;
                    int c = i % finalMaxCols;
                    if (r % 2 != 0) c = finalMaxCols - 1 - c;
                    
                    float px = startXCenter + c * colSpacing;
                    float py = startY + r * rowSpacing;
                    
                    stepWidgets.get(i).updateLayout(px, py, itemScale);
                }
            }
        }
        
        this.layoutDirty = false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible || manuscript == null) return;
        
        if (layoutDirty) {
            recalculateLayout();
        }
        
        renderBackground(guiGraphics);
        renderPreview(guiGraphics);
        renderSeparators(guiGraphics);
        renderIngredients(guiGraphics, mouseX, mouseY, partialTick);
        renderResults(guiGraphics);
        renderButtons(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x80000000);
        guiGraphics.fill(paperX + 2, paperY + 2, paperX + PAPER_WIDTH + 2, paperY + PAPER_HEIGHT + 2, 0x50000000);
        
        RenderSystem.setShaderTexture(0, MANUSCRIPT_PAPER);
        guiGraphics.blit(MANUSCRIPT_PAPER, paperX, paperY, 0, 0, PAPER_WIDTH, PAPER_HEIGHT, 104, 162);
        
        Font font = Minecraft.getInstance().font;
        String title = manuscript.name();
        int titleWidth = font.width(title);
        float titleScale = titleWidth > 80 ? 80.0f / titleWidth : 1.0f;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(paperX + PAPER_WIDTH / 2.0f, paperY + 12, 0);
        guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
        guiGraphics.drawString(font, title, -titleWidth / 2, 0, 0x4A3B2A, false);
        guiGraphics.pose().popPose();
    }

    private void renderPreview(GuiGraphics guiGraphics) {
        guiGraphics.renderOutline(previewX, previewY, previewW, previewH, 0x204A3B2A);

        StarChartRoute route = deductionResult != null ? deductionResult.route() : StarChartRoute.EMPTY;
        if (route != null && !route.segments().isEmpty()) {
            guiGraphics.flush(); 
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            
            guiGraphics.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 50); 
            
            PathRenderer.drawStaticPath(guiGraphics, route.segments(), previewX + 2, previewY + 2, previewW - 4, previewH - 4, 0xFF204080);
            
            guiGraphics.pose().popPose();
            
            guiGraphics.disableScissor();
            RenderSystem.enableDepthTest();
        } else {
             Font font = Minecraft.getInstance().font;
             guiGraphics.drawCenteredString(font, "?", previewX + previewW / 2, previewY + previewH / 2 - 4, 0x4A3B2A);
        }
    }
    
    private void renderSeparators(GuiGraphics guiGraphics) {
        // Preview Separator
        int sepY = previewY + previewH + 3;
        guiGraphics.fill(previewX + 5, sepY, previewX + previewW - 5, sepY + 1, 0x404A3B2A);
        
        // Result Separator
        Map<EffectField, PotionData> effects = deductionResult != null ? deductionResult.predictedEffects() : Map.of();
        if (!effects.isEmpty()) {
            int rSepY = resultStartY - 6;
            int inkColor = 0x604A3B2A;
            int cx = paperX + PAPER_WIDTH / 2;
            
            guiGraphics.fill(paperX + 15, rSepY, cx - 3, rSepY + 1, inkColor);
            guiGraphics.fill(cx + 4, rSepY, paperX + PAPER_WIDTH - 15, rSepY + 1, inkColor);
            
            guiGraphics.fill(cx, rSepY - 2, cx + 1, rSepY + 3, inkColor); 
            guiGraphics.fill(cx - 2, rSepY, cx + 3, rSepY + 1, inkColor); 
        }
    }

    private void renderIngredients(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int inputCount = stepWidgets.size();
        if (inputCount <= 0) return;
        
        int lineColor = 0xFF4A3B2A;
        
        // Draw connecting lines
        if (inputCount > 1) {
            for (int i = 0; i < inputCount - 1; i++) {
                ManuscriptStepWidget w1 = stepWidgets.get(i);
                ManuscriptStepWidget w2 = stepWidgets.get(i + 1);
                
                int x1 = (int)w1.getX();
                int y1 = (int)w1.getY();
                int x2 = (int)w2.getX();
                int y2 = (int)w2.getY();
                
                if (x1 == x2) { 
                     guiGraphics.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), lineColor);
                } else { 
                     guiGraphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, lineColor);
                }
            }
            
            // Draw Arrow
            ManuscriptStepWidget wLast = stepWidgets.get(inputCount - 1);
            ManuscriptStepWidget wPrev = stepWidgets.get(inputCount - 2);
            int pLastX = (int)wLast.getX();
            int pLastY = (int)wLast.getY();
            int pPrevX = (int)wPrev.getX();
            int pPrevY = (int)wPrev.getY();
            
            int mx = (pLastX + pPrevX) / 2;
            int my = (pLastY + pPrevY) / 2;
            int arrowSize = wLast.getScale() < 0.6f ? 1 : 2; 

            if (pLastX == pPrevX) { 
                 if (pLastY > pPrevY) { 
                     guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                     guiGraphics.fill(mx - 1, my - 1, mx + 2, my, lineColor);
                     if (arrowSize > 1) guiGraphics.fill(mx - 2, my - 2, mx + 3, my - 1, lineColor);
                 }
            } else if (pLastX > pPrevX) { 
                guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                guiGraphics.fill(mx, my - 1, mx + 1, my + 2, lineColor);
                if (arrowSize > 1) guiGraphics.fill(mx - 1, my - 2, mx, my + 3, lineColor);
            } else { 
                guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                guiGraphics.fill(mx, my - 1, mx + 1, my + 2, lineColor);
                if (arrowSize > 1) guiGraphics.fill(mx + 1, my - 2, mx + 2, my + 3, lineColor);
            }
        }
        
        // Render Widgets
        for (ManuscriptStepWidget widget : stepWidgets) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick); 
        }
    }
    
    private void renderResults(GuiGraphics guiGraphics) {
        Map<EffectField, PotionData> effects = deductionResult != null ? deductionResult.predictedEffects() : Map.of();
        if (effects.isEmpty()) return;
        
        Font font = Minecraft.getInstance().font;
        int startX = paperX + 12;
        int curX = startX;
        int curY = resultStartY + 2;
        int itemW = 28; 
        int maxRowW = PAPER_WIDTH - 24;
        
        for (Map.Entry<EffectField, PotionData> entry : effects.entrySet()) {
            EffectField field = entry.getKey();
            PotionData data = entry.getValue();
            
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(field.effect()).orElse(null);
            
            if (holder != null) {
                if (curX + itemW > startX + maxRowW) {
                    curX = startX;
                    curY += 14;
                }
                
                TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(holder);
                RenderSystem.setShaderTexture(0, sprite.atlasLocation());
                
                guiGraphics.blit(curX, curY, 0, 10, 10, sprite);
                
                if (data.level() > 0) {
                    String lvl = String.valueOf(data.level() + 1);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(curX + 8, curY, 200);
                    guiGraphics.pose().scale(0.5f, 0.5f, 1f);
                    guiGraphics.drawString(font, lvl, 0, 0, 0xFF000000, false);
                    guiGraphics.pose().popPose();
                }
                
                int totalSeconds = data.duration() / 20;
                String timeStr = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
                
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(curX + 12, curY + 2, 0);
                guiGraphics.pose().scale(0.6f, 0.6f, 1f);
                guiGraphics.drawString(font, timeStr, 0, 0, 0xFF4A3B2A, false);
                guiGraphics.pose().popPose();
                
                curX += itemW;
            }
        }
    }
    
    private void renderButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        closeButton.render(guiGraphics, mouseX, mouseY, partialTick);
        loadButton.render(guiGraphics, mouseX, mouseY, partialTick);
        deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        if (closeButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (loadButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (deleteButton.mouseClicked(mouseX, mouseY, button)) return true;
        
        // 点击外部关闭
        if (mouseX < paperX || mouseX > paperX + PAPER_WIDTH || mouseY < paperY || mouseY > paperY + PAPER_HEIGHT) {
            hide();
            return true;
        }
        
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
    
    // 内部类：纯文字按钮
    private class TextButton extends Button {
        private final int color;
        public TextButton(int x, int y, int width, int height, String label, int color, Consumer<Button> onPress) {
            super(x, y, width, height, Component.literal(label), onPress::accept, Button.DEFAULT_NARRATION);
            this.color = color;
        }
        
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int c = hovered ? color : (color & 0xAAFFFFFF); // 平时稍微透明一点
            
            Font font = Minecraft.getInstance().font;
            int textWidth = font.width(getMessage());
            int dx = (width - textWidth) / 2;
            int dy = (height - 8) / 2;
            
            guiGraphics.drawString(font, getMessage(), getX() + dx, getY() + dy, c, false);
            
            // 悬停下划线
            if (hovered) {
                guiGraphics.fill(getX() + dx, getY() + dy + 9, getX() + dx + textWidth, getY() + dy + 10, c);
            }
        }
    }
}
