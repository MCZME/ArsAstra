package com.github.mczme.arsastra.client.gui.widget.manuscript;

import com.github.mczme.arsastra.client.gui.tab.ManuscriptsTab;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.core.manuscript.ManuscriptManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 手稿书本组件
 * 负责在手稿页签中以书本形式展示所有保存的手稿，支持分页、搜索过滤和悬停预览。
 */
public class ManuscriptBookWidget extends AbstractWidget {
    private static final ResourceLocation MANUSCRIPT_ICONS = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_icons.png");

    private final ManuscriptsTab parentTab;
    private List<ClientManuscript> allManuscripts = new ArrayList<>();
    private int currentPage = 0;
    private final int itemsPerPage = 14; // 每页 7 条 * 2 页
    
    // 布局常量
    private final int leftPageXOffset = 15;
    private final int rightPageXOffset = 155;
    private final int pageYOffset = 20;
    private final int itemHeight = 20;
    private final int itemWidth = 120;

    public ManuscriptBookWidget(int x, int y, int width, int height, ManuscriptsTab parentTab) {
        super(x, y, width, height, Component.empty());
        this.parentTab = parentTab;
        refresh();
    }

    /**
     * 刷新手稿列表，应用工具栏的过滤条件。
     */
    public void refresh() {
        List<ClientManuscript> source = ManuscriptManager.getInstance().getManuscripts();
        ManuscriptToolbar toolbar = parentTab.getToolbar();

        if (toolbar != null) {
            String searchQuery = toolbar.getSearchQuery();
            java.util.Set<Integer> filterIcons = toolbar.getFilterIcons();
            String filterItem = toolbar.getFilterItem();
            String filterEffect = toolbar.getFilterEffect();

            this.allManuscripts = source.stream().filter(m -> {
                // 1. 搜索查询
                if (!searchQuery.isEmpty() && !m.name().toLowerCase().contains(searchQuery)) {
                    return false;
                }

                // 2. 图标过滤
                if (!filterIcons.isEmpty()) {
                    try {
                        int iconIndex = Integer.parseInt(m.icon());
                        if (!filterIcons.contains(iconIndex)) return false;
                    } catch (NumberFormatException ignored) {
                        return false;
                    }
                }

                // 3. 物品过滤
                if (!filterItem.isEmpty()) {
                    boolean hasItem = m.inputs().stream().anyMatch(input -> 
                        input.stack().getHoverName().getString().toLowerCase().contains(filterItem.toLowerCase())
                    );
                    if (!hasItem) return false;
                }

                // 4. 效果过滤
                if (!filterEffect.isEmpty()) {
                    boolean hasEffect = m.outcome().stream().anyMatch(line -> 
                        line.toLowerCase().contains(filterEffect.toLowerCase())
                    );
                    if (!hasEffect) return false;
                }

                return true;
            }).toList();
        } else {
            this.allManuscripts = new ArrayList<>(source);
        }
        
        this.currentPage = 0;
    }
    
    public void nextPage() {
        if ((currentPage + 1) * itemsPerPage < allManuscripts.size()) {
            currentPage++;
        }
    }
    
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allManuscripts.size());
        
        ClientManuscript hoveredManuscript = null;
        int hoveredX = 0;
        int hoveredY = 0;

        for (int i = start; i < end; i++) {
            ClientManuscript m = allManuscripts.get(i);
            int localIndex = i - start;
            boolean isRightPage = localIndex >= 7;
            int pageIndex = localIndex % 7;
            
            int itemX = this.getX() + (isRightPage ? rightPageXOffset : leftPageXOffset);
            int itemY = this.getY() + pageYOffset + (pageIndex * itemHeight);
            
            boolean isHovered = mouseX >= itemX && mouseX < itemX + itemWidth &&
                                mouseY >= itemY && mouseY < itemY + itemHeight;

            renderEntry(guiGraphics, m, itemX, itemY, isHovered);
            
            if (isHovered) {
                hoveredManuscript = m;
                hoveredX = mouseX;
                hoveredY = mouseY;
            }
        }
        
        // 分页导航渲染 (占位符)
        if (currentPage > 0) {
             guiGraphics.drawString(Minecraft.getInstance().font, "<", this.getX() + 20, this.getY() + this.height - 20, 0x404040, false);
        }
        if ((currentPage + 1) * itemsPerPage < allManuscripts.size()) {
             guiGraphics.drawString(Minecraft.getInstance().font, ">", this.getX() + this.width - 30, this.getY() + this.height - 20, 0x404040, false);
        }

        // 最后渲染悬停提示
        if (hoveredManuscript != null) {
            renderHoverTooltip(guiGraphics, hoveredManuscript, hoveredX, hoveredY);
        }
    }

    /**
     * 渲染单条手稿条目。
     */
    private void renderEntry(GuiGraphics guiGraphics, ClientManuscript m, int x, int y, boolean isHovered) {
        // 悬停时高亮背景
        if (isHovered) {
             guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, 0x10000000); 
        }
        
        // 图标
        int iconIndex = 0;
        try {
            iconIndex = Integer.parseInt(m.icon());
        } catch (NumberFormatException ignored) {}
        
        RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
        int u = (iconIndex % 4) * 16;
        int v = (iconIndex / 4) * 16;
        guiGraphics.blit(MANUSCRIPT_ICONS, x + 2, y + 2, u, v, 16, 16, 64, 64);
        
        // 文本
        int color = 0x333333; // 深灰色墨水色
        guiGraphics.drawString(Minecraft.getInstance().font, m.name(), x + 22, y + 6, color, false);
        
        // 分隔线
        guiGraphics.fill(x + 5, y + itemHeight - 1, x + itemWidth - 5, y + itemHeight, 0x20000000);
    }

    /**
     * 渲染悬停提示，展示手稿详情预览。
     */
    private void renderHoverTooltip(GuiGraphics guiGraphics, ClientManuscript m, int mouseX, int mouseY) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        
        // 1. 计算尺寸
        // 日期格式
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(m.createdAt()));

        int nameWidth = font.width(m.name());
        int dateWidth = font.width(dateStr);
        int headerWidth = nameWidth + 15 + dateWidth; // 名称和日期之间的间距
        
        int inputsWidth = 0;
        if (!m.inputs().isEmpty()) {
            int count = Math.min(m.inputs().size(), 7); // 最多 7 个槽位 (6个物品 + 1个溢出指示)
            inputsWidth = (count * 18) + 5; // 每个槽位 18px
        }
        
        int outcomesWidth = 0;
        for (String line : m.outcome()) {
            outcomesWidth = Math.max(outcomesWidth, font.width(line));
        }
        
        int tooltipWidth = Math.max(Math.max(headerWidth, inputsWidth), outcomesWidth) + 10;
        
        int tooltipHeight = 5 + 10 + 3; // 顶部边距 + 标题文本 + 标题下划线间距
        if (!m.inputs().isEmpty()) {
            tooltipHeight += 24; // 物品 (18) + 指示器空间 (4) + 边距 (2)
        }
        if (!m.outcome().isEmpty()) {
            tooltipHeight += 5 + (m.outcome().size() * 10);
        }
        
        int x = mouseX + 10;
        int y = mouseY + 10;
        
        // 越界调整
        if (x + tooltipWidth > Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
            x -= tooltipWidth + 20;
        }
        
        // 2. 绘制背景 (纸条风格)
        // 阴影
        guiGraphics.fill(x + 2, y + 2, x + tooltipWidth + 2, y + tooltipHeight + 2, 0x40000000);
        // 纸张底色
        guiGraphics.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xFFF0E6D2);
        // 简单的墨水边框 (非封闭，或者手绘感)
        guiGraphics.renderOutline(x, y, tooltipWidth, tooltipHeight, 0xFF5C4033);
        
        int currentY = y + 5;

        // 3. 绘制页眉 (名称 + 日期)
        guiGraphics.drawString(font, m.name(), x + 5, currentY, 0xFF4A3B2A, false);
        guiGraphics.drawString(font, dateStr, x + tooltipWidth - 5 - dateWidth, currentY, 0xFF887766, false);
        currentY += 10;
        
        // 分隔线
        guiGraphics.fill(x + 5, currentY + 1, x + tooltipWidth - 5, currentY + 2, 0x885C4033);
        currentY += 4;
        
        // 4. 绘制输入
        if (!m.inputs().isEmpty()) {
            int itemX = x + 5;
            int maxSlots = 7;
            boolean overflow = m.inputs().size() > maxSlots;
            int displayCount = overflow ? maxSlots - 1 : m.inputs().size();
            
            for (int i = 0; i < displayCount; i++) {
                var input = m.inputs().get(i);
                
                // 物品
                guiGraphics.renderItem(input.stack(), itemX, currentY + 3);
                // 旋转指示
                if (Math.abs(input.rotation()) > 0.001f) {
                     guiGraphics.pose().pushPose();
                     guiGraphics.pose().translate(itemX + 10, currentY + 2, 200);
                     guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);
                     guiGraphics.drawString(font, "↻", 0, 0, 0xFF8B2500, false);
                     guiGraphics.pose().popPose();
                }
                
                itemX += 18;
            }
            
            // 溢出指示器
            if (overflow) {
                 guiGraphics.drawString(font, "...", itemX + 4, currentY + 7, 0xFF4A3B2A, false);
            }
            
            currentY += 22;
        }
        
        // 5. 绘制结果
        if (!m.outcome().isEmpty()) {
            currentY += 2;
            for (String line : m.outcome()) {
                 guiGraphics.drawString(font, line, x + 5, currentY, 0xFF1E7636, false);
                 currentY += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 分页导航逻辑
        if (currentPage > 0 && mouseX >= this.getX() + 10 && mouseX <= this.getX() + 30 && mouseY >= this.getY() + this.height - 25 && mouseY <= this.getY() + this.height - 10) {
            prevPage();
            return true;
        }
        if ((currentPage + 1) * itemsPerPage < allManuscripts.size() && mouseX >= this.getX() + this.width - 40 && mouseX <= this.getX() + this.width - 20 && mouseY >= this.getY() + this.height - 25 && mouseY <= this.getY() + this.height - 10) {
            nextPage();
            return true;
        }

        // 条目点击逻辑
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allManuscripts.size());
        
        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            boolean isRightPage = localIndex >= 7;
            int pageIndex = localIndex % 7;
            
            int itemX = this.getX() + (isRightPage ? rightPageXOffset : leftPageXOffset);
            int itemY = this.getY() + pageYOffset + (pageIndex * itemHeight);
            
            if (mouseX >= itemX && mouseX < itemX + itemWidth &&
                mouseY >= itemY && mouseY < itemY + itemHeight) {
                parentTab.onSelect(allManuscripts.get(i));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // TODO: 实现叙述功能
    }
}