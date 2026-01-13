package com.github.mczme.arsastra.client.gui.tab;

import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.client.gui.logic.ItemFilterLogic;
import com.github.mczme.arsastra.client.gui.util.PathRenderer;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarFilterWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarSearchWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarWidget;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.engine.service.RouteGenerationService;
import com.github.mczme.arsastra.core.starchart.engine.service.RouteGenerationServiceImpl;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.registry.AARegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CompendiumTab implements JournalTab {
    private enum DisplayMode { ITEMS, ELEMENTS }

    private PlayerKnowledge knowledge;
    private final RouteGenerationService routeService = new RouteGenerationServiceImpl();
    
    // UI 组件
    private ToolbarWidget toolbar;
    private ToolbarTabButton modeSwitchButton;
    private ToolbarFilterWidget filterWidget;
    
    // 布局参数
    private int x, y;

    // 数据字段 - 物品模式
    private List<ItemStack> allAnalyzedItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private ItemStack selectedItem = ItemStack.EMPTY;

    // 数据字段 - 要素模式
    private List<Element> allElements = new ArrayList<>();
    private List<Element> filteredElements = new ArrayList<>();
    private Element selectedElement = null;
    
    // 状态字段
    private DisplayMode currentMode = DisplayMode.ITEMS;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 20;
    private String currentSearchQuery = "";

    @Override
    public void init(StarChartJournalScreen screen, int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.knowledge = screen.getPlayerKnowledge();

        // 初始化工具栏
        this.toolbar = new ToolbarWidget(x + 15, y - 13, 200, 22);
        
        // 1. 模式切换按钮 (初始为 ITEMS 模式)
        this.modeSwitchButton = new ToolbarTabButton(0, 0, 20, 22, Component.empty(), 2, 0x4040A0, this::toggleMode);
        this.toolbar.addChild(this.modeSwitchButton);

        // 2. 搜索组件
        this.toolbar.addChild(new ToolbarSearchWidget(0, 0, (query) -> {
            this.currentSearchQuery = query.toLowerCase();
            this.refreshContent();
        }));

        // 3. 筛选组件 (高级筛选)
        this.filterWidget = new ToolbarFilterWidget(0, 0, this::refreshContent);
        this.toolbar.addChild(this.filterWidget);
        
        this.toolbar.visible = false;
        screen.addTabWidget(this.toolbar);

        refreshContent();
    }

    private void toggleMode() {
        if (this.currentMode == DisplayMode.ITEMS) {
            this.currentMode = DisplayMode.ELEMENTS;
            this.modeSwitchButton.setIconIndex(3);
            this.modeSwitchButton.setColor(0x804080);
        } else {
            this.currentMode = DisplayMode.ITEMS;
            this.modeSwitchButton.setIconIndex(2);
            this.modeSwitchButton.setColor(0x4040A0);
        }
        this.currentPage = 0;
        refreshContent();
    }

    public void refreshContent() {
        if (currentMode == DisplayMode.ITEMS) {
            refreshItems();
        } else {
            refreshElements();
        }
    }

    private void refreshItems() {
        if (knowledge == null) return;
        
        // 直接从玩家已分析的物品开始，过滤掉没有配置的物品
        this.allAnalyzedItems = knowledge.getAnalyzedItems().stream()
                .map(id -> BuiltInRegistries.ITEM.get(id))
                .filter(item -> item != Items.AIR)
                .filter(item -> ElementProfileManager.getInstance().getElementProfile(item).isPresent())
                .map(ItemStack::new)
                .collect(Collectors.toList());
        
        // 2. 准备筛选器
        Predicate<ItemStack> advancedFilter = ItemFilterLogic.create(
            filterWidget.getElementFilter(), 
            filterWidget.getTagFilter()
        );
        
        // 3. 应用筛选 (高级筛选 + 搜索词)
        this.filteredItems = this.allAnalyzedItems.stream()
                .filter(advancedFilter)
                .filter(stack -> {
                    if (currentSearchQuery.isEmpty()) return true;
                    return stack.getHoverName().getString().toLowerCase().contains(currentSearchQuery);
                })
                .collect(Collectors.toList());
        
        // 4. 更新选中项
        if (selectedItem.isEmpty() && !filteredItems.isEmpty()) {
            selectedItem = filteredItems.get(0);
        } else if (!filteredItems.contains(selectedItem) && !filteredItems.isEmpty()) {
            selectedItem = filteredItems.get(0);
        }
    }

    private void refreshElements() {
        this.allElements = AARegistries.ELEMENT_REGISTRY.stream().collect(Collectors.toList());
        
        if (!currentSearchQuery.isEmpty()) {
            this.filteredElements = this.allElements.stream()
                    .filter(e -> Component.translatable(e.getDescriptionId()).getString().toLowerCase().contains(currentSearchQuery))
                    .collect(Collectors.toList());
        } else {
            this.filteredElements = this.allElements;
        }

        if (selectedElement == null && !filteredElements.isEmpty()) {
            selectedElement = filteredElements.get(0);
        } else if (!filteredElements.contains(selectedElement) && !filteredElements.isEmpty()) {
            selectedElement = filteredElements.get(0);
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (currentMode == DisplayMode.ITEMS) {
            renderItemGrid(guiGraphics, mouseX, mouseY);
            renderItemDetails(guiGraphics, mouseX, mouseY);
        } else {
            renderElementGrid(guiGraphics, mouseX, mouseY);
            renderElementDetails(guiGraphics);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (toolbar != null) {
            toolbar.visible = visible;
        }
    }

    private void renderItemGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = this.x + 25;
        int startY = this.y + 20; // 调整 Y 坐标以适应新布局
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            ItemStack stack = filteredItems.get(i);
            
            if (selectedItem.getItem() == stack.getItem()) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x40AA8800); 
            } else if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x20000000); 
            }
            guiGraphics.renderFakeItem(stack, slotX, slotY);
        }
        renderPaginator(guiGraphics, filteredItems.size());
    }

    private void renderElementGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = this.x + 25;
        int startY = this.y + 20;
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredElements.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            Element element = filteredElements.get(i);
            
            if (selectedElement == element) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x40804080);
            } else if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                guiGraphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0x20000000); 
            }
            
            RenderSystem.enableBlend();
            guiGraphics.blit(element.getIcon(), slotX, slotY, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }
        renderPaginator(guiGraphics, filteredElements.size());
    }
    
    private void renderPaginator(GuiGraphics guiGraphics, int totalItems) {
        if (totalItems > ITEMS_PER_PAGE) {
            String pageText = (currentPage + 1) + " / " + (int) Math.ceil(totalItems / (double) ITEMS_PER_PAGE);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, pageText, this.x + 70, this.y + 145, 0x404040);
            if (currentPage > 0) guiGraphics.drawString(Minecraft.getInstance().font, "<", this.x + 30, this.y + 145, 0x404040, false);
            if ((currentPage + 1) * ITEMS_PER_PAGE < totalItems) guiGraphics.drawString(Minecraft.getInstance().font, ">", this.x + 110, this.y + 145, 0x404040, false);
        }
    }

    private void renderItemDetails(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (selectedItem.isEmpty()) return;
        int rightX = this.x + 160;
        int topY = this.y + 20;

        // 1. 顶部：物品大图标
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(rightX + 60, topY + 30, 0);
        guiGraphics.pose().scale(2.5f, 2.5f, 2.5f);
        guiGraphics.renderFakeItem(selectedItem, -8, -8);
        guiGraphics.pose().popPose();

        // 2. 顶部：物品名称
        Component name = selectedItem.getHoverName();
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, name, rightX + 60, topY + 55, 0x000000);
        
        // 分割线
        guiGraphics.fill(rightX + 10, topY + 68, rightX + 110, topY + 69, 0xFF6B4E38);

        // --- 下方内容区域 (左右分栏) ---
        int contentStartY = topY + 75;
        int leftColX = rightX + 10;
        int rightColX = rightX + 70; // 右侧栏起始 X (向右平移 5 像素)

        // 3. 左侧：要素列表
        ElementProfileManager.getInstance().getElementProfile(selectedItem.getItem()).ifPresent(profile -> {
            int elementY = contentStartY;
            for (var entry : profile.elements().entrySet()) {
                Element element = AARegistries.ELEMENT_REGISTRY.get(entry.getKey());
                if (element != null) {
                    // 检测悬停 (限制宽度为 60 像素，避免覆盖右侧预览框)
                    boolean isHovered = mouseX >= leftColX && mouseX < leftColX + 60 && mouseY >= elementY && mouseY < elementY + 12;
                    if (isHovered) {
                        guiGraphics.fill(leftColX - 2, elementY, leftColX + 60, elementY + 11, 0x15000000);
                        guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.compendium.click_to_view"), mouseX, mouseY);
                    }

                    guiGraphics.blit(element.getIcon(), leftColX, elementY, 0, 0, 10, 10, 10, 10);
                    String nameText = Component.translatable(element.getDescriptionId()).getString();
                    String text = String.format("%s: %.1f", nameText, entry.getValue());
                    guiGraphics.drawString(Minecraft.getInstance().font, text, leftColX + 12, elementY + 1, isHovered ? 0x000000 : 0x333333, false);
                    elementY += 12; 
                }
            }
        });

        // 4. 右侧：轨迹预览框
        int previewSize = 40;
        int previewX = rightColX;
        int previewY = contentStartY; // 与要素列表顶对齐

        // 绘制边框 (移除背景填充)
        guiGraphics.renderOutline(previewX, previewY, previewSize, previewSize, 0xFF6B4E38);

        // 绘制中心原点
        int cx = previewX + previewSize / 2;
        int cy = previewY + previewSize / 2;
        guiGraphics.fill(cx - 1, cy, cx + 2, cy + 1, 0xFF888888);
        guiGraphics.fill(cx, cy - 1, cx + 1, cy + 2, 0xFF888888);

        // 渲染路径
        List<StarChartPath> paths = routeService.getPathsForItem(selectedItem);
        if (!paths.isEmpty()) {
            float maxDist = 0;
            Vector2f finalDisplacement = new Vector2f(0, 0);

            for (StarChartPath p : paths) {
                finalDisplacement.add(p.getEndPoint()); // 理想路径终点即位移
                for (Vector2f point : p.sample(1.0f)) {
                     float d = point.length();
                     if (d > maxDist) maxDist = d;
                }
            }

            if (maxDist < 0.1f) {
                guiGraphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF333333); // 改为深色
            } else {
                guiGraphics.flush(); // 确保之前的渲染（图标等）已提交
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.disableCull();

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                // 抬高 Z 轴，确保在背景层之上
                poseStack.translate(cx, cy, 100.0f);
                
                float targetRadius = (previewSize / 2.0f) * 0.8f;
                float scale = targetRadius / maxDist;
                scale = Math.min(scale, 10.0f); 

                poseStack.scale(scale, scale, 1.0f);

                for (StarChartPath path : paths) {
                    // 使用白色顶点色配合同步回来的 PathRenderer (它内部用 Multiply 混合)
                    // 采样步长加密到 0.2
                    PathRenderer.renderPencilPath(poseStack, path.sample(0.2f / scale), 3.0f / scale, 0xFFFFFFFF, 1.0f);
                }
                
                poseStack.popPose();
                RenderSystem.enableDepthTest();

                // 绘制缩放比例 (预览框右下角内部)
                String zoomText = String.format("%.1fx", scale);
                int tw = Minecraft.getInstance().font.width(zoomText);
                guiGraphics.pose().pushPose();
                // 抬高 Z 轴确保可见，并缩小字体
                guiGraphics.pose().translate(previewX + previewSize - tw * 0.5f - 1, previewY + previewSize - 6, 110.0f);
                guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
                guiGraphics.drawString(Minecraft.getInstance().font, zoomText, 0, 0, 0x666666, false);
                guiGraphics.pose().popPose();
            }
        } else {
             guiGraphics.drawCenteredString(Minecraft.getInstance().font, "-", cx, cy - 4, 0xFFAAAAAA);
        }
    }

    private void renderElementDetails(GuiGraphics guiGraphics) {
        if (selectedElement == null) return;
        int rightX = this.x + 160;
        int topY = this.y + 20;

        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(rightX + 60, topY + 30, 0);
        guiGraphics.pose().scale(2.5f, 2.5f, 2.5f);
        guiGraphics.blit(selectedElement.getIcon(), -8, -8, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();

        Component name = Component.translatable(selectedElement.getDescriptionId());
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, name, rightX + 60, topY + 55, 0x000000);
        guiGraphics.fill(rightX + 10, topY + 68, rightX + 110, topY + 69, 0xFF6B4E38);
        
        guiGraphics.drawWordWrap(Minecraft.getInstance().font, Component.literal("This is a placeholder description for element " + name.getString()), rightX + 10, topY + 75, 100, 0x333333);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int startX = this.x + 25;
        int startY = this.y + 20;
        
        int totalItems = (currentMode == DisplayMode.ITEMS) ? filteredItems.size() : filteredElements.size();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            int relIndex = i - startIndex;
            int col = relIndex % 5;
            int row = relIndex / 5;
            int slotX = startX + col * 22;
            int slotY = startY + row * 22;

            // 扩大点击范围以匹配高亮背景 (从 slotX-2 到 slotX+18)
            if (mouseX >= slotX - 2 && mouseX < slotX + 18 && mouseY >= slotY - 2 && mouseY < slotY + 18) {
                if (currentMode == DisplayMode.ITEMS) {
                    this.selectedItem = filteredItems.get(i);
                } else {
                    this.selectedElement = filteredElements.get(i);
                }
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        
        // 翻页逻辑
        if (mouseY >= this.y + 140 && mouseY <= this.y + 160) {
            if (mouseX >= this.x + 20 && mouseX <= this.x + 50 && currentPage > 0) {
                currentPage--;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            if (mouseX >= this.x + 90 && mouseX <= this.x + 120 && (currentPage + 1) * ITEMS_PER_PAGE < totalItems) {
                currentPage++;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        // 要素列表点击跳转逻辑
        if (currentMode == DisplayMode.ITEMS && !selectedItem.isEmpty()) {
            int rightX = this.x + 160;
            int topY = this.y + 20;
            int contentStartY = topY + 75;
            int leftColX = rightX + 10;

            var profileOpt = ElementProfileManager.getInstance().getElementProfile(selectedItem.getItem());
            if (profileOpt.isPresent()) {
                int elementY = contentStartY;
                for (var entry : profileOpt.get().elements().entrySet()) {
                    Element element = AARegistries.ELEMENT_REGISTRY.get(entry.getKey());
                    if (element != null) {
                        if (mouseX >= leftColX && mouseX < leftColX + 110 && mouseY >= elementY && mouseY < elementY + 12) {
                            this.currentMode = DisplayMode.ELEMENTS;
                            this.selectedElement = element;
                            this.currentSearchQuery = ""; // 清除搜索以确保要素可见
                            this.currentPage = 0;
                            this.modeSwitchButton.setIconIndex(3);
                            this.modeSwitchButton.setColor(0x804080);
                            refreshContent();
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            return true;
                        }
                        elementY += 12;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (toolbar != null && toolbar.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (toolbar != null && toolbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (toolbar != null && toolbar.charTyped(codePoint, modifiers)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (toolbar != null && toolbar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (toolbar != null && toolbar.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        return false;
    }
}