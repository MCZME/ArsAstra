package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.network.payload.AnalysisActionPayload;
import com.github.mczme.arsastra.registry.AARegistries;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.client.gui.util.StarChartRenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分析台 GUI 屏幕
 */
public class AnalysisDeskScreen extends AbstractContainerScreen<AnalysisDeskMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/analysis_desk.png");
    private static final ResourceLocation TEXTURE_0 = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/analysis_desk_0.png");

    private final List<VerticalElementSlider> sliders = new ArrayList<>();

    private boolean wasResearching = false;
    private ItemStack lastStack = ItemStack.EMPTY;

    public AnalysisDeskScreen(AnalysisDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 0;
        this.inventoryLabelY = 110;
        rebuildCustomWidgets();
    }

    /**
     * 重建自定义小部件（按钮和滑块）
     */
    private void rebuildCustomWidgets() {
        this.clearWidgets();
        this.sliders.clear();

        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        boolean isResearching = be.isResearching();
        ItemStack stack = be.getItemHandler().getStackInSlot(0);

        // 按钮区域：物品槽 (12, 20) 下方稍右
        int btnX = this.leftPos + 4;
        int btnStartY = this.topPos + 50;
        int btnWidth = 36;
        int btnHeight = 12;

        if (!isResearching) {
            if (!stack.isEmpty()) {
                // 直接分析
                this.addRenderableWidget(new LensButton(btnX, btnStartY, btnWidth, btnHeight, 
                        Component.translatable("gui.ars_astra.analysis.btn_direct"), Palette.BTN_DIRECT, button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.DIRECT_ANALYSIS, Map.of()));
                }));

                // 开始猜测 (直觉路径)
                this.addRenderableWidget(new LensButton(btnX, btnStartY + 16, btnWidth, btnHeight, 
                        Component.translatable("gui.ars_astra.analysis.btn_intuition"), Palette.BTN_INTUITION, button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.START_GUESS, Map.of()));
                }));
            }
        } else {
            // 滑块区域：右侧
            Optional<ElementProfile> profile = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
            if (profile.isPresent()) {
                int sliderX = this.leftPos + 80;
                int sliderY = this.topPos + 20;
                int sliderSpacing = 22; 

                for (ResourceLocation elementId : profile.get().elements().keySet()) {
                    VerticalElementSlider slider = new VerticalElementSlider(sliderX, sliderY, 18, 85, elementId);
                    this.addRenderableWidget(slider);
                    this.sliders.add(slider);
                    sliderX += sliderSpacing;
                }

                // 提交
                this.addRenderableWidget(new LensButton(btnX, btnStartY, btnWidth, btnHeight, 
                        Component.translatable("gui.ars_astra.analysis.btn_submit"), Palette.BTN_SUBMIT, button -> {
                    Map<ResourceLocation, AnalysisActionPayload.GuessData> guesses = new HashMap<>();
                    for (VerticalElementSlider s : sliders) {
                        guesses.put(s.elementId, new AnalysisActionPayload.GuessData(s.getValueInt(), s.isPrecise));
                    }
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.SUBMIT_GUESS, guesses));
                }));

                // 直接分析 (在猜测中也可以使用)
                this.addRenderableWidget(new LensButton(btnX, btnStartY + 16, btnWidth, btnHeight, 
                        Component.translatable("gui.ars_astra.analysis.btn_direct"), Palette.BTN_DIRECT, button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.DIRECT_ANALYSIS, Map.of()));
                }));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        boolean isResearching = be.isResearching();
        ItemStack currentStack = be.getItemHandler().getStackInSlot(0);

        if (isResearching != wasResearching || !ItemStack.matches(lastStack, currentStack)) {
            this.wasResearching = isResearching;
            this.lastStack = currentStack.copy();
            this.rebuildCustomWidgets();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        
        renderFeedbackIcons(guiGraphics);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderFeedbackIcons(GuiGraphics guiGraphics) {
        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        if (be.isResearching()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.ars_astra.analysis.guesses_left", be.getGuessesRemaining()), this.leftPos + 10, this.topPos + 100, 0xFF5555, false);

            Map<ResourceLocation, Integer> feedback = be.getLastFeedback();
            for (VerticalElementSlider slider : sliders) {
                if (feedback.containsKey(slider.elementId)) {
                    int val = feedback.get(slider.elementId);
                    String icon = "";
                    int color = 0xFFFFFF;
                    if (val < 0) { icon = "↑"; color = 0xFFAA00; }
                    else if (val > 0) { icon = "↓"; color = 0xFF5555; }
                    else { icon = "√"; color = 0x55FF55; }
                    
                    // 绘制在滑块上方 (绝对坐标)
                    // slider.getX() 是绝对坐标
                    guiGraphics.drawCenteredString(this.font, icon, slider.getX() + slider.getWidth() / 2, slider.getY() - 10, color);
                }
            }
        }
    }

    /**
     * 自定义透镜风格按钮
     */
    private class LensButton extends Button {
        private final int baseColor;

        public LensButton(int x, int y, int width, int height, Component message, int color, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.baseColor = color;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int alpha = 0xCC000000;
            int color = baseColor & 0x00FFFFFF;
            
            int displayColor = alpha | color;
            if (isHovered) {
                 displayColor = 0xEE000000 | lighten(color, 1.2f);
            }

            int borderColor = Palette.BRASS;
            
            RenderSystem.enableBlend();
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, displayColor);
            guiGraphics.renderOutline(getX(), getY(), width, height, borderColor);
            
            // 缩放文字以适应小型按钮
            guiGraphics.pose().pushPose();
            float scale = 0.7f;
            guiGraphics.pose().translate(getX() + width / 2.0f, getY() + (height - 8 * scale) / 2.0f + 1.0f, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            
            int textColor = 0xFFFFFF;
            int textWidth = font.width(getMessage());
            guiGraphics.drawString(font, getMessage(), -textWidth / 2, 0, textColor, false);
            guiGraphics.pose().popPose();
            
            RenderSystem.disableBlend();
        }
        
        private int lighten(int rgb, float factor) {
            int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * factor);
            int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * factor);
            int b = (int) Math.min(255, (rgb & 0xFF) * factor);
            return (r << 16) | (g << 8) | b;
        }
    }

    /**
     * 自定义要素滑块（垂直方向，手绘风格）
     */
    private class VerticalElementSlider extends AbstractWidget {
        private final ResourceLocation elementId;
        private double value = 0.5;
        private boolean isPrecise = true;
        private boolean isDragging = false;
        
        private static final int ICON_SIZE = 12; // 缩小图标
        private static final int SLIDER_AREA_HEIGHT = 64; // 滑动区域高度

        public VerticalElementSlider(int x, int y, int width, int height, ResourceLocation elementId) {
            super(x, y, width, height, Component.empty());
            this.elementId = elementId;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 1. 绘制轨道 (源 83x382 -> 目标 14x64)
            int trackWidth = 14;
            int trackHeight = SLIDER_AREA_HEIGHT;
            int trackX = getX() + (width - trackWidth) / 2;
            guiGraphics.blit(TEXTURE_0, trackX, getY(), trackWidth, trackHeight, 0, 0, 83, 382, 85, 555);

            int trackCenter = getX() + width / 2;
            int trackTop = getY();
            int trackBottom = getY() + trackHeight; 
            int handleY = trackBottom - (int)(value * trackHeight);

            // 2. 如果是范围模式，绘制范围指示器
            if (!isPrecise) {
                int rangeW = 10;
                int rangeH = 24;
                int rangeX = trackCenter - rangeW / 2;
                int rangeY = handleY - rangeH / 2;
                rangeY = Mth.clamp(rangeY, trackTop, trackBottom - rangeH);
                guiGraphics.blit(TEXTURE_0, rangeX, rangeY, rangeW, rangeH, 0, 388, 54, 127, 85, 555);
            } else {
                // 3. 仅在精确模式下绘制滑块游标
                int knobW = 14;
                int knobH = 6;
                int knobX = trackCenter - knobW / 2;
                int knobY = handleY - knobH / 2;
                guiGraphics.blit(TEXTURE_0, knobX, knobY, knobW, knobH, 0, 520, 77, 35, 85, 555);
            }

            // 4. 绘制要素图标及其边框 (使用 StarChartRenderUtils)
            int borderSize = 16; 
            int borderX = trackCenter - borderSize / 2;
            int borderY = getY() + SLIDER_AREA_HEIGHT + 4; 
            
            boolean mouseOverIcon = isHovered && mouseY >= (getY() + SLIDER_AREA_HEIGHT);

            int borderColor = isPrecise ? Palette.BRASS : 0xFF00FFFF;
            if (mouseOverIcon) {
                borderColor = isPrecise ? 0xFFFFEEAA : 0xFF88FFFF;
            }

            // 准备矩形顶点
            List<Vector2f> vertices = List.of(
                new Vector2f(borderX, borderY),
                new Vector2f(borderX + borderSize, borderY),
                new Vector2f(borderX + borderSize, borderY + borderSize),
                new Vector2f(borderX, borderY + borderSize)
            );

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 1.0f); // 稍微抬高 Z 轴，防止与背景 Z-fighting
            RenderSystem.disableCull(); // 确保不剔除双面几何体

            if (mouseOverIcon) {
                guiGraphics.fill(borderX, borderY, borderX + borderSize, borderY + borderSize, 0x22FFFFFF);
            }

            // 绘制要素图标 (12x12) - 在高亮之上，边框之下
            int iconX = borderX + (borderSize - ICON_SIZE) / 2;
            int iconY = borderY + (borderSize - ICON_SIZE) / 2;
            ResourceLocation texture = AARegistries.ELEMENT_REGISTRY.get(elementId).getIcon();
            guiGraphics.blit(texture, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // 绘制动态手绘边框
            StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), vertices, borderColor, 1.2f);
            
            RenderSystem.enableCull(); // 恢复状态
            guiGraphics.pose().popPose();
            
            RenderSystem.disableBlend();
            
            if (isHovered) {
                 if (mouseY < trackBottom) {
                     Component valueText;
                     int val = getValueInt();
                     if (isPrecise) {
                         valueText = Component.literal(String.valueOf(val));
                     } else {
                         int min = Math.max(0, val - 15);
                         int max = Math.min(100, val + 15);
                         valueText = Component.literal(min + " - " + max);
                     }
                     guiGraphics.renderTooltip(font, valueText, mouseX, mouseY);
                 } else if (mouseOverIcon) {
                     var element = AARegistries.ELEMENT_REGISTRY.get(elementId);
                     Component name = Component.translatable(element.getDescriptionId());
                     Component mode = isPrecise ? 
                         Component.translatable("gui.ars_astra.analysis.mode.precise") : 
                         Component.translatable("gui.ars_astra.analysis.mode.range");
                     
                     guiGraphics.renderTooltip(font, List.of(name, mode), Optional.empty(), mouseX, mouseY);
                 }
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (mouseY > getY() + SLIDER_AREA_HEIGHT) {
                // 点击图标区域：切换精确/范围模式
                isPrecise = !isPrecise;
                playDownSound(Minecraft.getInstance().getSoundManager());
            } else {
                // 点击滑动区域：设置值
                this.isDragging = true;
                setValueFromMouse(mouseY);
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.isDragging = false;
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            if (this.isDragging) {
                setValueFromMouse(mouseY);
            }
        }
        
        private void setValueFromMouse(double mouseY) {
            double relativeY = mouseY - getY();
            double trackHeight = 64;
            double val = 1.0 - (relativeY / trackHeight);
            this.value = Mth.clamp(val, 0.0, 1.0);
        }

        public int getValueInt() {
            return (int) (this.value * 100);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}