package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopViewModel;
import com.github.mczme.arsastra.client.gui.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SequenceStripWidget extends FloatingWidget {
    private final WorkshopViewModel viewModel;
    private final DragHandler dragHandler;
    private int scrollOffset = 0;
    private static final int SLOT_SIZE = 20;
    private static final int GAP = 12; // 增加间距以放置箭头

    public SequenceStripWidget(int x, int y, int width, WorkshopViewModel viewModel, DragHandler dragHandler) {
        super(x, y, width, 40, Component.translatable("gui.ars_astra.workshop.sequence"));
        this.viewModel = viewModel;
        this.dragHandler = dragHandler;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 绘制背景 (羊皮纸风格)
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Palette.PARCHMENT_BG);
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Palette.INK);

        // 2. 绘制时间轴底线 (贯穿全条)
        int centerY = getY() + getHeight() / 2;
        guiGraphics.fill(getX() + 5, centerY, getX() + getWidth() - 5, centerY + 1, Palette.INK_LIGHT);

        List<ItemStack> sequence = viewModel.getSequence();
        int startX = getX() + 15 - scrollOffset;

        // 启用剪裁以防止滚动内容溢出
        guiGraphics.enableScissor(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2);

        for (int i = 0; i <= sequence.size(); i++) {
            int slotX = startX + i * (SLOT_SIZE + GAP);
            int slotY = centerY - SLOT_SIZE / 2;

            if (i < sequence.size()) {
                // 渲染已有的物品节点
                renderSlot(guiGraphics, slotX, slotY, sequence.get(i), mouseX, mouseY, false);
                
                // 在节点之间绘制指向右侧的小箭头
                drawArrow(guiGraphics, slotX + SLOT_SIZE + 2, centerY);
            } else {
                // 渲染序列末尾的占位符 (带有 "+" 号的虚线感槽位)
                renderSlot(guiGraphics, slotX, slotY, ItemStack.EMPTY, mouseX, mouseY, true);
            }
        }

        // 3. 绘制插入指示器 (仅在拖拽且鼠标在组件内时)
        if (dragHandler.isDragging() && isMouseOver(mouseX, mouseY)) {
            int dropIndex = calculateDropIndex(mouseX);
            // 计算指示线的位置：应该在对应索引的槽位左侧
            int indicatorX = startX + dropIndex * (SLOT_SIZE + GAP) - (GAP / 2);
            
            // 绘制朱砂色的垂直指示线，高度覆盖整个组件高度
            guiGraphics.fill(indicatorX, getY() + 8, indicatorX + 1, getY() + getHeight() - 8, Palette.CINNABAR);
        }

        guiGraphics.disableScissor();
    }

    /**
     * 渲染单个物品槽位
     */
    private void renderSlot(GuiGraphics guiGraphics, int x, int y, ItemStack stack, int mouseX, int mouseY, boolean isPlaceholder) {
        boolean isHovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        
        // 节点底色 (遮挡背后的轴线)
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, Palette.PARCHMENT_BG);

        if (isPlaceholder) {
            // 占位符：虚线轮廓 + 淡墨色加号
            guiGraphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, Palette.INK_LIGHT);
            guiGraphics.drawString(Minecraft.getInstance().font, "+", x + 7, y + 6, Palette.INK_LIGHT, false);
        } else {
            // 物品槽：实线轮廓 + 装饰角标
            guiGraphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, Palette.INK);
            // 装饰角标 (手绘感)
            guiGraphics.fill(x, y, x + 2, y + 1, Palette.INK); // 左上
            guiGraphics.fill(x, y, x + 1, y + 2, Palette.INK);
            guiGraphics.fill(x + SLOT_SIZE - 2, y, x + SLOT_SIZE, y + 1, Palette.INK); // 右上
            
            guiGraphics.renderFakeItem(stack, x + 2, y + 2);

            if (isHovered && !dragHandler.isDragging()) {
                guiGraphics.renderOutline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Palette.CINNABAR);
                guiGraphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            }
        }
    }

    /**
     * 绘制连接箭头
     */
    private void drawArrow(GuiGraphics guiGraphics, int x, int centerY) {
        int arrowColor = Palette.INK_LIGHT;
        // 简单的 3x3 箭头形状
        guiGraphics.fill(x, centerY, x + 4, centerY + 1, arrowColor);
        guiGraphics.fill(x + 2, centerY - 2, x + 3, centerY + 3, arrowColor);
        guiGraphics.fill(x + 3, centerY - 1, x + 4, centerY + 2, arrowColor);
    }

    private int calculateDropIndex(double mouseX) {
        int listSize = viewModel.getSequence().size();
        int startX = getX() + 15 - scrollOffset;
        int relativeX = (int) mouseX - startX;
        
        // 计算逻辑：根据相对于起点的位置，除以槽位宽+间距
        // 四舍五入到最近的索引位置
        int index = (relativeX + (SLOT_SIZE + GAP) / 2) / (SLOT_SIZE + GAP);
        return Math.max(0, Math.min(index, listSize));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            // 滚轮控制左右滚动
            scrollOffset -= (int) (scrollY * 20);
            // 限制滚动范围
            int totalWidth = viewModel.getSequence().size() * (SLOT_SIZE + GAP);
            int maxScroll = Math.max(0, totalWidth - getWidth() + 40);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean isMouseOverTitle(double mouseX, double mouseY) {
        return false; // 序列条不支持通过标题拖拽移动
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0 && !dragHandler.isDragging()) {
            List<ItemStack> sequence = viewModel.getSequence();
            int centerY = getY() + getHeight() / 2;
            int startX = getX() + 15 - scrollOffset;

            for (int i = 0; i < sequence.size(); i++) {
                int slotX = startX + i * (SLOT_SIZE + GAP);
                int slotY = centerY - SLOT_SIZE / 2;

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    dragHandler.startDrag(sequence.get(i));
                    viewModel.removeFromSequence(i);
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && dragHandler.isDragging()) {
            int index = calculateDropIndex(mouseX);
            ItemStack stack = dragHandler.getDraggingStack();
            
            viewModel.insertToSequence(index, stack);
            dragHandler.endDrag();
            
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
