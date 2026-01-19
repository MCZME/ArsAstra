package com.github.mczme.arsastra.client.gui;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.network.payload.AnalysisActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AnalysisDeskScreen extends AbstractContainerScreen<AnalysisDeskMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/analysis_desk.png");

    private Button btnDirect;
    private Button btnIntuition;
    private Button btnSubmit;
    private Button btnQuit;
    private final List<ElementSlider> sliders = new ArrayList<>();

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
        this.titleLabelY = 10;
        this.inventoryLabelY = 110;
        rebuildCustomWidgets();
    }

    private void rebuildCustomWidgets() {
        this.clearWidgets();
        this.sliders.clear();

        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        boolean isResearching = be.isResearching();
        ItemStack stack = be.getItemHandler().getStackInSlot(0);

        int startX = this.leftPos + 40;
        int startY = this.topPos + 20;

        if (!isResearching) {
            // 常规模式：显示两个入口按钮
            // 只有当有物品且未分析时才可用 (这里简化为有物品就显示)
            if (!stack.isEmpty()) {
                this.btnDirect = this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.analysis.btn_direct"), button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.DIRECT_ANALYSIS, Map.of()));
                }).bounds(startX, startY + 50, 60, 20).build());

                this.btnIntuition = this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.analysis.btn_intuition"), button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.START_GUESS, Map.of()));
                }).bounds(startX + 65, startY + 50, 60, 20).build());
            } else {
                // 没有物品，显示提示或空
            }
        } else {
            // 猜测模式：显示 Slider 和操作按钮
            Optional<ElementProfile> profile = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
            if (profile.isPresent()) {
                int sliderY = startY;
                for (ResourceLocation elementId : profile.get().elements().keySet()) {
                    // 获取要素名称 (简单处理，实际应从 Registry 获取 Localized Name)
                    Component elemName = Component.translatable("element." + elementId.getNamespace() + "." + elementId.getPath());
                    ElementSlider slider = new ElementSlider(this.leftPos + 80, sliderY, 80, 20, elemName, 0, elementId);
                    this.addRenderableWidget(slider);
                    this.sliders.add(slider);
                    sliderY += 24;
                }

                this.btnSubmit = this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.analysis.btn_submit"), button -> {
                    Map<ResourceLocation, Integer> guesses = new HashMap<>();
                    for (ElementSlider s : sliders) {
                        guesses.put(s.elementId, s.getValueInt());
                    }
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.SUBMIT_GUESS, guesses));
                }).bounds(startX, startY + 80, 50, 20).build());

                this.btnQuit = this.addRenderableWidget(Button.builder(Component.translatable("gui.ars_astra.analysis.btn_quit"), button -> {
                    PacketDistributor.sendToServer(new AnalysisActionPayload(be.getBlockPos(), AnalysisActionPayload.Action.QUIT_GUESS, Map.of()));
                }).bounds(startX + 55, startY + 80, 50, 20).build());
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        boolean isResearching = be.isResearching();
        ItemStack currentStack = be.getItemHandler().getStackInSlot(0);

        // 检测状态变化或物品变化，触发 UI 重建
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
        
        AnalysisDeskBlockEntity be = this.menu.blockEntity;
        if (be.isResearching()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.ars_astra.analysis.guesses_left", be.getGuessesRemaining()), 10, 20, 0xFF5555, false);
            
            // 渲染滑块旁的反馈
            Map<ResourceLocation, Integer> feedback = be.getLastFeedback();
            for (ElementSlider slider : sliders) {
                if (feedback.containsKey(slider.elementId)) {
                    int val = feedback.get(slider.elementId);
                    String icon = "";
                    int color = 0xFFFFFF;
                    if (val < 0) { icon = "↑"; color = 0xFFAA00; } // 猜小了，提示 ↑
                    else if (val > 0) { icon = "↓"; color = 0xFF5555; } // 猜大了，提示 ↓
                    else { icon = "√"; color = 0x55FF55; } // 正确
                    
                    guiGraphics.drawString(this.font, icon, slider.getX() + slider.getWidth() + 5, slider.getY() + 6, color, false);
                }
            }
        }
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
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // 自定义滑块类
    private static class ElementSlider extends AbstractSliderButton {
        private final ResourceLocation elementId;
        private final Component elementName;

        public ElementSlider(int x, int y, int width, int height, Component elementName, double value, ResourceLocation elementId) {
            super(x, y, width, height, Component.empty(), value);
            this.elementId = elementId;
            this.elementName = elementName;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("%s: %s", elementName, getValueInt()));
        }

        @Override
        protected void applyValue() {
            // 值变化时的逻辑，这里不需要实时发包
        }
        
        public int getValueInt() {
            return (int) (this.value * 100);
        }
    }
}
