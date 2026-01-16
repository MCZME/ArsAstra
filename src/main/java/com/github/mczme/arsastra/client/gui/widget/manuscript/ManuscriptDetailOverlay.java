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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import org.joml.Vector2f;

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
    private DeductionResult deductionResult; // Store result from service
    
    private final List<ManuscriptStepWidget> stepWidgets = new ArrayList<>();

    // 纸张尺寸和位置 (104x162)
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
        
        // 关闭按钮 (右上角小叉)
        this.closeButton = new TextButton(centerX + PAPER_WIDTH - 15, centerY + 8, 10, 10, "x", 0xFF000000, b -> hide());
            
        // 加载按钮 (底部居右文字)
        this.loadButton = new TextButton(centerX + PAPER_WIDTH - 40, centerY + PAPER_HEIGHT - 25, 30, 15, "Apply", 0xFF1E7636, b -> {
            if (manuscript != null) {
                if (parentTab.getScreen().getTab(1) instanceof WorkshopTab workshopTab) {
                    workshopTab.getSession().loadSequence(manuscript.inputs());
                    parentTab.getScreen().switchTab(1);
                    hide();
                }
            }
        });

        // 删除按钮 (底部居左文字)
        this.deleteButton = new TextButton(centerX + 10, centerY + PAPER_HEIGHT - 25, 30, 15, "Discard", 0xFF8B2500, b -> {
            if (!confirmDelete) {
                confirmDelete = true;
                b.setMessage(Component.literal("Sure?"));
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
        this.deleteButton.setMessage(Component.literal("Discard"));
        
        // Initialize step widgets
        this.stepWidgets.clear();
        for (int i = 0; i < manuscript.inputs().size(); i++) {
            this.stepWidgets.add(new ManuscriptStepWidget(manuscript.inputs().get(i), i));
        }
        
        StarChartManager.getInstance().getStarChart(ResourceLocation.fromNamespaceAndPath("ars_astra", "base_chart"))
            .ifPresent(chart -> {
                this.deductionResult = deductionService.deduce(chart, manuscript.inputs());
            });
    }
    
    public void hide() {
        this.visible = false;
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible || manuscript == null) return;
        
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x80000000);
        
        int x = (screenWidth - PAPER_WIDTH) / 2;
        int y = (screenHeight - PAPER_HEIGHT) / 2;
        
        // 纸张阴影
        guiGraphics.fill(x + 2, y + 2, x + PAPER_WIDTH + 2, y + PAPER_HEIGHT + 2, 0x50000000);

        // 纸张背景
        RenderSystem.setShaderTexture(0, MANUSCRIPT_PAPER);
        guiGraphics.blit(MANUSCRIPT_PAPER, x, y, 0, 0, PAPER_WIDTH, PAPER_HEIGHT, 104, 162);
        
        Font font = Minecraft.getInstance().font;
        
        // 1. 标题
        String title = manuscript.name();
        int titleWidth = font.width(title);
        float titleScale = titleWidth > 80 ? 80.0f / titleWidth : 1.0f;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + PAPER_WIDTH / 2.0f, y + 12, 0);
        guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
        guiGraphics.drawString(font, title, -titleWidth / 2, 0, 0x4A3B2A, false);
        guiGraphics.pose().popPose();

        int inputCount = manuscript.inputs().size();
        
        // 2. 星图预览 (高度减半)
        int previewH = inputCount > 42 ? 20 : 27;
        int previewX = x + 12;
        int previewY = y + 28;
        int previewW = PAPER_WIDTH - 24;
        
        guiGraphics.renderOutline(previewX, previewY, previewW, previewH, 0x204A3B2A);

        StarChartRoute route = deductionResult != null ? deductionResult.route() : StarChartRoute.EMPTY;
        if (route != null && !route.segments().isEmpty()) {
            RenderSystem.enableBlend();
            guiGraphics.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
            PathRenderer.drawStaticPath(guiGraphics, route.segments(), previewX + 2, previewY + 2, previewW - 4, previewH - 4, 0xFF204080);
            guiGraphics.disableScissor();
        } else {
             guiGraphics.drawCenteredString(font, "?", previewX + previewW / 2, previewY + previewH / 2 - 4, 0x4A3B2A);
        }

        // 3. 结果区域计算
        int resultHeight = 0;
        int resultStartY = y + 137; // 默认在按钮区上方
        
        Map<EffectField, PotionData> effects = deductionResult != null ? deductionResult.predictedEffects() : Map.of();
        if (!effects.isEmpty()) {
            int resultRows = (effects.size() + 2) / 3; // 假设每行 3 个
            resultHeight = Math.max(20, resultRows * 14); 
            resultStartY = y + 137 - resultHeight;
        }

        // 4. 原料区 (自适应布局，填充 Preview 和 Result 之间的空间)
        if (inputCount > 0) {
            // 可用区域: 从 Preview Bottom 到 Result Top
            int contentStartY = previewY + previewH + 4; // padding
            int contentEndY = resultStartY - 2; // padding
            int availableHeight = contentEndY - contentStartY;
            
            // 自适应参数
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
            
            // 垂直居中
            int centerY = contentStartY + availableHeight / 2;
            int rawStartY = centerY - totalBlockHeight / 2;
            final int startY = Math.max(rawStartY, contentStartY);
            
            int startXCenter = x + (PAPER_WIDTH - totalBlockWidth) / 2;
            
            final int finalMaxCols = maxCols;
            java.util.function.IntFunction<int[]> getPos = (i) -> {
                int r = i / finalMaxCols;
                int c = i % finalMaxCols;
                if (r % 2 != 0) c = finalMaxCols - 1 - c;
                return new int[]{startXCenter + c * colSpacing, startY + r * rowSpacing};
            };
            
            int lineColor = 0xFF4A3B2A;

            // 绘制连线
            if (inputCount > 1) {
                for (int i = 0; i < inputCount - 1; i++) {
                    int[] p1 = getPos.apply(i);
                    int[] p2 = getPos.apply(i + 1);
                    int x1 = p1[0], y1 = p1[1];
                    int x2 = p2[0], y2 = p2[1];
                    
                    if (x1 == x2) { 
                         guiGraphics.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), lineColor);
                    } else { 
                         guiGraphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, lineColor);
                    }
                }
                
                // 箭头
                int[] pLast = getPos.apply(inputCount - 1);
                int[] pPrev = getPos.apply(inputCount - 2);
                int mx = (pLast[0] + pPrev[0]) / 2;
                int my = (pLast[1] + pPrev[1]) / 2;
                int arrowSize = itemScale < 0.6f ? 1 : 2; 

                if (pLast[0] == pPrev[0]) { 
                     if (pLast[1] > pPrev[1]) { 
                         guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                         guiGraphics.fill(mx - 1, my - 1, mx + 2, my, lineColor);
                         if (arrowSize > 1) guiGraphics.fill(mx - 2, my - 2, mx + 3, my - 1, lineColor);
                     }
                } else if (pLast[0] > pPrev[0]) { 
                    guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                    guiGraphics.fill(mx, my - 1, mx + 1, my + 2, lineColor);
                    if (arrowSize > 1) guiGraphics.fill(mx - 1, my - 2, mx, my + 3, lineColor);
                } else { 
                    guiGraphics.fill(mx, my, mx + 1, my + 1, lineColor);
                    guiGraphics.fill(mx, my - 1, mx + 1, my + 2, lineColor);
                    if (arrowSize > 1) guiGraphics.fill(mx + 1, my - 2, mx + 2, my + 3, lineColor);
                }
            }

            // 更新并渲染步骤组件
            for (int i = 0; i < inputCount; i++) {
                if (i < stepWidgets.size()) {
                    int[] pos = getPos.apply(i);
                    ManuscriptStepWidget widget = stepWidgets.get(i);
                    widget.updateLayout(pos[0], pos[1], itemScale);
                    widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
        }
        
        // 5. 绘制结果
        if (!effects.isEmpty()) {
            int startX = x + 12;
            int curX = startX;
            int curY = resultStartY + 2;
            int itemW = 28; // 图标+简短文字
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
        
        // 4. 按钮
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
        int x = (screenWidth - PAPER_WIDTH) / 2;
        int y = (screenHeight - PAPER_HEIGHT) / 2;
        if (mouseX < x || mouseX > x + PAPER_WIDTH || mouseY < y || mouseY > y + PAPER_HEIGHT) {
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
        private final int hoverColor;
        
        public TextButton(int x, int y, int width, int height, String label, int color, Consumer<Button> onPress) {
            super(x, y, width, height, Component.literal(label), onPress::accept, Button.DEFAULT_NARRATION);
            this.color = color;
            // 悬停时颜色变浅或变亮
            this.hoverColor = (color & 0xFF000000) | ((color & 0x00FEFEFE) >> 1) + (color & 0x00808080);
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
