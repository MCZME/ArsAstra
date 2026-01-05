package com.github.mczme.arsastra.client.gui.widget.compendium;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CompendiumFilterWidget extends AbstractWidget {
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/journal_tabs.png");
    
    private final List<FilterTab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;
    private final Consumer<Integer> onFilterChanged;

    public CompendiumFilterWidget(int x, int y, Consumer<Integer> onFilterChanged) {
        super(x, y, 120, 20, Component.empty());
        this.onFilterChanged = onFilterChanged;

        // 初始化分类标签 (占位图标)
        tabs.add(new FilterTab(0, new ItemStack(Items.COMPASS), "all"));        // 全部
        tabs.add(new FilterTab(1, new ItemStack(Items.GRASS_BLOCK), "nature")); // 自然
        tabs.add(new FilterTab(2, new ItemStack(Items.IRON_PICKAXE), "tools"));  // 工具
        tabs.add(new FilterTab(3, new ItemStack(Items.BLAZE_POWDER), "alch")); // 炼金材料
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < tabs.size(); i++) {
            FilterTab tab = tabs.get(i);
            boolean isActive = (activeTabIndex == i);
            
            int tabX = this.getX() + 15 + i * 24;
            int tabY = this.getY() - (isActive ? 20 : 16); // 选中时向上伸出更多
            int tabWidth = 20;
            int tabHeight = isActive ? 24 : 20;

            // 绘制底板 (使用与侧边标签类似的逻辑，但 UV 不同或旋转)
            // 这里我们暂时使用代码绘制，直到有专门的顶部标签贴图
            int color = isActive ? 0xFFE7D6B5 : 0xFF8B5E3C;
            guiGraphics.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, 0xFF3D2C1E); // 边框
            guiGraphics.fill(tabX + 1, tabY + (isActive ? 0 : 1), tabX + tabWidth - 1, tabY + tabHeight, color);

            // 绘制图标
            guiGraphics.renderFakeItem(tab.icon, tabX + 2, tabY + (isActive ? 4 : 2));
            
            // 悬停提示
            if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                // 原则上应该在 Screen 层渲染 Tooltip，这里先记录
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = this.getX() + 15 + i * 24;
            int tabY = this.getY() - 20;
            if (mouseX >= tabX && mouseX < tabX + 20 && mouseY >= tabY && mouseY < this.getY()) {
                this.activeTabIndex = i;
                this.onFilterChanged.accept(i);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private static record FilterTab(int id, ItemStack icon, String translationKey) {}
}
