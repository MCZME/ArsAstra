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

public class ManuscriptBookWidget extends AbstractWidget {
    private static final ResourceLocation MANUSCRIPT_ICONS = ResourceLocation.fromNamespaceAndPath("ars_astra", "textures/gui/manuscript_icons.png");

    private final ManuscriptsTab parentTab;
    private List<ClientManuscript> allManuscripts = new ArrayList<>();
    private int currentPage = 0;
    private final int itemsPerPage = 14; // 7 per page * 2 pages
    
    // Layout constants
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

    public void refresh() {
        List<ClientManuscript> source = ManuscriptManager.getInstance().getManuscripts();
        ManuscriptToolbar toolbar = parentTab.getToolbar();

        if (toolbar != null) {
            String searchQuery = toolbar.getSearchQuery();
            String elementFilter = toolbar.getElementFilter();
            String tagFilter = toolbar.getTagFilter();

            this.allManuscripts = source.stream().filter(m -> {
                // 1. Search Query
                if (!searchQuery.isEmpty() && !m.name().toLowerCase().contains(searchQuery)) {
                    return false;
                }

                // 2. Element Filter
                if (!elementFilter.isEmpty()) {
                    ResourceLocation elementId = ResourceLocation.tryParse(elementFilter);
                    if (elementId != null) {
                        boolean hasElement = m.inputs().stream().anyMatch(input ->
                            com.github.mczme.arsastra.core.element.profile.ElementProfileManager.getInstance()
                                .getElementProfile(input.stack().getItem())
                                .map(profile -> profile.elements().containsKey(elementId))
                                .orElse(false)
                        );
                        if (!hasElement) return false;
                    }
                }

                // 3. Tag Filter
                if (!tagFilter.isEmpty()) {
                    ResourceLocation tagId = ResourceLocation.tryParse(tagFilter);
                    if (tagId != null) {
                        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagKey = 
                            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                        boolean hasTag = m.inputs().stream().anyMatch(input -> input.stack().is(tagKey));
                        if (!hasTag) return false;
                    }
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
        
        // Simple page navigation rendering (placeholders)
        if (currentPage > 0) {
             guiGraphics.drawString(Minecraft.getInstance().font, "<", this.getX() + 20, this.getY() + this.height - 20, 0x404040, false);
        }
        if ((currentPage + 1) * itemsPerPage < allManuscripts.size()) {
             guiGraphics.drawString(Minecraft.getInstance().font, ">", this.getX() + this.width - 30, this.getY() + this.height - 20, 0x404040, false);
        }

        // Render tooltip last
        if (hoveredManuscript != null) {
            renderHoverTooltip(guiGraphics, hoveredManuscript, hoveredX, hoveredY);
        }
    }

    private void renderEntry(GuiGraphics guiGraphics, ClientManuscript m, int x, int y, boolean isHovered) {
        // Highlight background on hover
        if (isHovered) {
             guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, 0x10000000); 
        }
        
        // Icon
        int iconIndex = 0;
        try {
            iconIndex = Integer.parseInt(m.icon());
        } catch (NumberFormatException ignored) {}
        
        RenderSystem.setShaderTexture(0, MANUSCRIPT_ICONS);
        int u = (iconIndex % 4) * 16;
        int v = (iconIndex / 4) * 16;
        guiGraphics.blit(MANUSCRIPT_ICONS, x + 2, y + 2, u, v, 16, 16, 64, 64);
        
        // Text
        int color = 0x333333; // Dark grey ink color
        guiGraphics.drawString(Minecraft.getInstance().font, m.name(), x + 22, y + 6, color, false);
        
        // Divider line
        guiGraphics.fill(x + 5, y + itemHeight - 1, x + itemWidth - 5, y + itemHeight, 0x20000000);
    }

    private void renderHoverTooltip(GuiGraphics guiGraphics, ClientManuscript m, int mouseX, int mouseY) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        
        // 1. Calculate Dimensions
        // Date Format
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(m.createdAt()));

        int nameWidth = font.width(m.name());
        int dateWidth = font.width(dateStr);
        int headerWidth = nameWidth + 15 + dateWidth; // Space between name and date
        
        int inputsWidth = 0;
        if (!m.inputs().isEmpty()) {
            int count = Math.min(m.inputs().size(), 7); // Max 7 slots (6 + 1 indicator if overflow)
            inputsWidth = (count * 18) + 5; // 18px per slot
        }
        
        int outcomesWidth = 0;
        for (String line : m.outcome()) {
            outcomesWidth = Math.max(outcomesWidth, font.width(line));
        }
        
        int tooltipWidth = Math.max(Math.max(headerWidth, inputsWidth), outcomesWidth) + 10;
        
        int tooltipHeight = 5 + 10 + 3; // Top padding + Header text + Header underline spacing
        if (!m.inputs().isEmpty()) {
            tooltipHeight += 24; // Items (18) + Indicator space (4) + padding (2)
        }
        if (!m.outcome().isEmpty()) {
            tooltipHeight += 5 + (m.outcome().size() * 10);
        }
        
        int x = mouseX + 10;
        int y = mouseY + 10;
        
        // Adjust if off-screen
        if (x + tooltipWidth > Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
            x -= tooltipWidth + 20;
        }
        
        // 2. Draw Background
        guiGraphics.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xFFFDF5E6);
        guiGraphics.renderOutline(x, y, tooltipWidth, tooltipHeight, 0xFF8B4513); // Brown border
        
        int currentY = y + 5;

        // 3. Draw Header (Name + Date)
        guiGraphics.drawString(font, m.name(), x + 5, currentY, 0x000000, false);
        guiGraphics.drawString(font, dateStr, x + tooltipWidth - 5 - dateWidth, currentY, 0x555555, false);
        currentY += 10;
        
        // Separator
        guiGraphics.fill(x + 3, currentY + 1, x + tooltipWidth - 3, currentY + 2, 0xFF8B4513);
        currentY += 4;
        
        // 4. Draw Inputs
        if (!m.inputs().isEmpty()) {
            int itemX = x + 5;
            int maxSlots = 7;
            boolean overflow = m.inputs().size() > maxSlots;
            int displayCount = overflow ? maxSlots - 1 : m.inputs().size();
            
            for (int i = 0; i < displayCount; i++) {
                var input = m.inputs().get(i);
                
                // Rotation Indicator (Red square above)
                if (Math.abs(input.rotation()) > 0.001f) {
                     guiGraphics.fill(itemX + 7, currentY, itemX + 9, currentY + 2, 0xFFFF0000);
                }
                
                // Item
                guiGraphics.renderItem(input.stack(), itemX, currentY + 3);
                itemX += 18;
            }
            
            // Overflow Indicator
            if (overflow) {
                 guiGraphics.drawString(font, "...", itemX + 4, currentY + 7, 0x000000, false);
            }
            
            currentY += 22;
        }
        
        // 5. Draw Outcome
        if (!m.outcome().isEmpty()) {
            currentY += 2;
            for (String line : m.outcome()) {
                 guiGraphics.drawString(font, line, x + 5, currentY, 0x2E8B57, false);
                 currentY += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Page navigation logic
        if (currentPage > 0 && mouseX >= this.getX() + 10 && mouseX <= this.getX() + 30 && mouseY >= this.getY() + this.height - 25 && mouseY <= this.getY() + this.height - 10) {
            prevPage();
            return true;
        }
        if ((currentPage + 1) * itemsPerPage < allManuscripts.size() && mouseX >= this.getX() + this.width - 40 && mouseX <= this.getX() + this.width - 20 && mouseY >= this.getY() + this.height - 25 && mouseY <= this.getY() + this.height - 10) {
            nextPage();
            return true;
        }

        // Entry click logic
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
