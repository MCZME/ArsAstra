package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.tab.ManuscriptsTab;
import com.github.mczme.arsastra.client.gui.tab.WorkshopTab;
import com.github.mczme.arsastra.client.gui.util.PathRenderer;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.List;
import java.util.Map;

public class ManuscriptDetailOverlay extends AbstractWidget {
    private static final ResourceLocation PARCHMENT_BG = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/parchment_background.png");
    
    private final ManuscriptsTab parentTab;
    private ClientManuscript manuscript;
    private final Runnable onClose;
    private final int screenWidth;
    private final int screenHeight;
    
    private Button closeButton;
    private Button loadButton;
    private Button deleteButton;
    
    private boolean confirmDelete = false;
    
    private final StarChartEngine engine = new StarChartEngineImpl();
    private StarChartRoute computedRoute = StarChartRoute.EMPTY;

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
        int paperWidth = 250;
        int paperHeight = 200;
        int centerX = (screenWidth - paperWidth) / 2;
        int centerY = (screenHeight - paperHeight) / 2;
        
        // Close Button (top-right of paper)
        this.closeButton = Button.builder(Component.literal("X"), b -> hide())
            .bounds(centerX + paperWidth - 20, centerY + 5, 15, 15)
            .build();
            
        // Load Button (bottom center-left)
        this.loadButton = Button.builder(Component.translatable("gui.ars_astra.load"), b -> {
            if (manuscript != null) {
                if (parentTab.getScreen().getTab(1) instanceof WorkshopTab workshopTab) {
                    workshopTab.getSession().loadSequence(manuscript.inputs());
                    parentTab.getScreen().switchTab(1);
                    hide();
                }
            }
        })
        .bounds(centerX + 40, centerY + paperHeight - 30, 80, 20)
        .build();

        // Delete Button (bottom center-right)
        this.deleteButton = Button.builder(Component.translatable("gui.ars_astra.delete"), b -> {
            if (!confirmDelete) {
                confirmDelete = true;
                b.setMessage(Component.translatable("gui.ars_astra.confirm").withStyle(net.minecraft.ChatFormatting.RED));
            } else {
                if (manuscript != null) {
                    ManuscriptManager.getInstance().deleteManuscript(manuscript.name());
                    parentTab.refreshBook();
                    hide();
                }
            }
        })
        .bounds(centerX + 130, centerY + paperHeight - 30, 80, 20)
        .build();
    }
    
    public void show(ClientManuscript manuscript) {
        this.manuscript = manuscript;
        this.visible = true;
        this.confirmDelete = false;
        this.deleteButton.setMessage(Component.translatable("gui.ars_astra.delete"));
        
        // Compute preview route
        StarChartManager.getInstance().getStarChart(ResourceLocation.fromNamespaceAndPath("ars_astra", "base_chart"))
            .ifPresent(chart -> {
                StarChartContext context = new StarChartContext(manuscript.inputs(), StarChartRoute.EMPTY, List.of(), 1.0f, Map.of());
                StarChartContext result = engine.compute(chart, context, new Vector2f(0, 0));
                this.computedRoute = result.currentRoute();
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
        
        // 1. Fullscreen dark overlay
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0x80000000);
        
        // 2. Paper Background
        int paperWidth = 250;
        int paperHeight = 200;
        int x = (screenWidth - paperWidth) / 2;
        int y = (screenHeight - paperHeight) / 2;
        
        RenderSystem.setShaderTexture(0, PARCHMENT_BG);
        guiGraphics.blit(PARCHMENT_BG, x, y, 0, 0, paperWidth, paperHeight, 256, 256);
        
        // 3. Header
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, manuscript.name(), x + paperWidth / 2, y + 10, 0x333333);
        
        // 4. Content Areas
        
        // Left: Inputs
        int leftPanelX = x + 15;
        int leftPanelY = y + 30;
        int leftPanelW = 105;
        // guiGraphics.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelW, leftPanelY + 90, 0x10000000); // Debug bg
        
        if (!manuscript.inputs().isEmpty()) {
            int itemX = leftPanelX;
            int itemY = leftPanelY;
            for (int i = 0; i < manuscript.inputs().size(); i++) {
                AlchemyInput input = manuscript.inputs().get(i);
                
                // Rotation Indicator
                if (Math.abs(input.rotation()) > 0.001f) {
                     guiGraphics.fill(itemX + 7, itemY, itemX + 9, itemY + 2, 0xFFFF0000);
                }
                
                guiGraphics.renderItem(input.stack(), itemX, itemY + 3);
                
                itemX += 18;
                if (itemX + 16 > leftPanelX + leftPanelW) {
                    itemX = leftPanelX;
                    itemY += 20;
                }
                
                if (itemY > leftPanelY + 70) break; // Overflow protection
            }
        }

        // Right: Path Preview
        int rightPanelX = x + 125;
        int rightPanelY = y + 30;
        int rightPanelW = 110;
        int rightPanelH = 90;
        
        guiGraphics.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelW, rightPanelY + rightPanelH, 0xFFF0F0F0); // Light paper inset
        guiGraphics.renderOutline(rightPanelX, rightPanelY, rightPanelW, rightPanelH, 0x20000000);
        
        if (computedRoute != null && !computedRoute.segments().isEmpty()) {
            guiGraphics.flush(); // Ensure background is drawn
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            
            PathRenderer.drawStaticPath(guiGraphics, computedRoute.segments(), rightPanelX, rightPanelY, rightPanelW, rightPanelH, 0xFF4080FF);
            
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        } else {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "?", rightPanelX + rightPanelW / 2, rightPanelY + rightPanelH / 2 - 4, 0x888888);
        }
        
        // Bottom: Outcomes
        int bottomPanelX = x + 15;
        int bottomPanelY = y + 130;
        int bottomPanelW = 220;
        
        // Separator line
        guiGraphics.fill(bottomPanelX, bottomPanelY - 5, bottomPanelX + bottomPanelW, bottomPanelY - 4, 0xFF8B4513);
        
        if (!manuscript.outcome().isEmpty()) {
            int currentY = bottomPanelY;
            for (String line : manuscript.outcome()) {
                 guiGraphics.drawString(Minecraft.getInstance().font, line, bottomPanelX, currentY, 0x2E8B57, false);
                 currentY += 10;
                 if (currentY > bottomPanelY + 30) break; // Limit lines
            }
        }
        
        // 5. Buttons
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
        
        int paperWidth = 250;
        int paperHeight = 200;
        int x = (screenWidth - paperWidth) / 2;
        int y = (screenHeight - paperHeight) / 2;
        
        if (mouseX < x || mouseX > x + paperWidth || mouseY < y || mouseY > y + paperHeight) {
            hide();
            return true;
        }
        
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
