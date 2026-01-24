package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SequenceStripWidget extends FloatingWidget {
    private final WorkshopSession session;
    private final DragHandler dragHandler;
    private int scrollOffset = 0;
    private boolean isScrolling = false; // 是否正在拖动滚动条
    private static final int SLOT_SIZE = 20;
    private static final int GAP = 12; // 增加间距以放置箭头
    private static final int SCROLLBAR_HEIGHT = 4; // 滚动条高度
    private static final int HIGHLIGHT_COLOR = 0xFFD4AF37; // 选中高亮色 (金色)
    
    // 拖拽逻辑状态
    private int pendingDragIndex = -1;
    private double clickX = 0;
    private double clickY = 0;
    private static final double DRAG_THRESHOLD = 3.0;

    public SequenceStripWidget(int x, int y, int width, WorkshopSession session, DragHandler dragHandler) {
        super(x, y, width, 40, Component.translatable("gui.ars_astra.workshop.sequence"));
        this.session = session;
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

        List<AlchemyInput> sequence = session.getInputs(); // 改用 getInputs 以访问旋转信息
        int contentWidth = (sequence.size() + 1) * (SLOT_SIZE + GAP) + 30; // 预留额外空间
        int maxScroll = Math.max(0, contentWidth - getWidth());
        
        // 限制滚动偏移
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        int startX = getX() + 15 - scrollOffset;

        // 启用剪裁以防止滚动内容溢出
        guiGraphics.enableScissor(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2);

        for (int i = 0; i <= sequence.size(); i++) {
            // 如果已达到最大输入限制，不显示末尾的占位符
            if (i == sequence.size() && sequence.size() >= session.getMaxInput()) {
                break;
            }

            int slotX = startX + i * (SLOT_SIZE + GAP);
            int slotY = centerY - SLOT_SIZE / 2;

            if (i < sequence.size()) {
                // 渲染已有的物品节点
                boolean isSelected = (i == session.getSelectedIndex());
                renderSlot(guiGraphics, slotX, slotY, sequence.get(i), mouseX, mouseY, false, isSelected);
                
                // 在节点之间绘制指向右侧的小箭头
                drawArrow(guiGraphics, slotX + SLOT_SIZE + 2, centerY);
            } else {
                // 渲染序列末尾的占位符 (带有 "+" 号的虚线感槽位)
                renderSlot(guiGraphics, slotX, slotY, null, mouseX, mouseY, true, false);
            }
        }

        // 3. 绘制插入指示器和幽灵预览 (仅在拖拽且鼠标在组件内时)
        if (dragHandler.isDragging() && isMouseOver(mouseX, mouseY)) {
            int dropIndex = calculateDropIndex(mouseX);
            // 更新会话预览状态，触发路径推演
            session.setPreview(dropIndex, dragHandler.getDraggingStack());

            // 计算指示线的位置：应该在对应索引的槽位左侧
            int indicatorX = startX + dropIndex * (SLOT_SIZE + GAP) - (GAP / 2);
            
            // 绘制朱砂色的垂直指示线，高度覆盖整个组件高度
            guiGraphics.fill(indicatorX, getY() + 4, indicatorX + 1, getY() + getHeight() - 4, Palette.CINNABAR);
            
            // 额外高亮：如果正好悬停在某个槽位上，绘制一个外框
            int hoverSlotX = startX + dropIndex * (SLOT_SIZE + GAP);
            // 即使是末尾的新位置，也应该显示预览
            
            // 幽灵预览坐标
            int ghostX = hoverSlotX;
            int ghostY = centerY - SLOT_SIZE / 2;

            // 绘制幽灵物品
            guiGraphics.renderFakeItem(dragHandler.getDraggingStack(), ghostX + 2, ghostY + 2);

            // 绘制半透明遮罩 (白色，约 50% 透明度)
            RenderSystem.enableBlend();
            guiGraphics.fill(ghostX, ghostY, ghostX + SLOT_SIZE, ghostY + SLOT_SIZE, 0x80FFFFFF);
            RenderSystem.disableBlend();

            // 绘制边框
            guiGraphics.renderOutline(ghostX - 1, ghostY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Palette.CINNABAR);
        } else if (dragHandler.isDragging() && !isMouseOver(mouseX, mouseY)) {
            // 如果正在拖拽但鼠标离开了当前组件，且不是在画布上（这个逻辑会在画布组件中处理），
            // 这里我们不做强制清除，因为可能用户正移向画布。
        }
        
        guiGraphics.disableScissor();

        // 4. 绘制滚动条 (仅当内容超出可视范围时)
        if (maxScroll > 0) {
            int scrollbarY = getY() + getHeight() - SCROLLBAR_HEIGHT - 2;
            int trackWidth = getWidth() - 10;
            int trackX = getX() + 5;
            
            // 绘制轨道
            guiGraphics.fill(trackX, scrollbarY, trackX + trackWidth, scrollbarY + SCROLLBAR_HEIGHT, Palette.INK_LIGHT & 0x80FFFFFF); // 半透明轨道
            
            // 计算滑块
            float ratio = (float) getWidth() / contentWidth;
            int thumbWidth = Math.max(20, (int) (trackWidth * ratio));
            int thumbX = trackX + (int) ((float) scrollOffset / maxScroll * (trackWidth - thumbWidth));
            
            // 绘制滑块 (正常为墨色，拖动/悬停为朱砂色)
            int thumbColor = (isScrolling || (mouseY >= scrollbarY && mouseY <= scrollbarY + SCROLLBAR_HEIGHT)) ? Palette.CINNABAR : Palette.INK;
            guiGraphics.fill(thumbX, scrollbarY, thumbX + thumbWidth, scrollbarY + SCROLLBAR_HEIGHT, thumbColor);
        }
    }

    /**
     * 渲染单个物品槽位
     */
    private void renderSlot(GuiGraphics guiGraphics, int x, int y, AlchemyInput input, int mouseX, int mouseY, boolean isPlaceholder, boolean isSelected) {
        boolean isHovered = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        ItemStack stack = isPlaceholder ? ItemStack.EMPTY : input.stack();
        
        // 节点底色 (遮挡背后的轴线)
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, Palette.PARCHMENT_BG);

        if (isPlaceholder) {
            // 占位符：虚线轮廓 + 淡墨色加号
            guiGraphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, Palette.INK_LIGHT);
            guiGraphics.drawString(Minecraft.getInstance().font, "+", x + 7, y + 6, Palette.INK_LIGHT, false);
        } else {
            // 物品槽：实线轮廓 + 装饰角标
            int outlineColor = isSelected ? HIGHLIGHT_COLOR : Palette.INK;
            guiGraphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, outlineColor);
            
            // 装饰角标 (手绘感)
            guiGraphics.fill(x, y, x + 2, y + 1, outlineColor); // 左上
            guiGraphics.fill(x, y, x + 1, y + 2, outlineColor);
            guiGraphics.fill(x + SLOT_SIZE - 2, y, x + SLOT_SIZE, y + 1, outlineColor); // 右上
            
            guiGraphics.renderFakeItem(stack, x + 2, y + 2);

            // 如果未选中且有旋转，显示旋转标记
            if (!isSelected && Math.abs(input.rotation()) > 0.001f) {
                // 在物品上方绘制一个小点或符号
                // 这里用一个小矩形表示
                guiGraphics.fill(x + SLOT_SIZE / 2 - 1, y - 3, x + SLOT_SIZE / 2 + 1, y - 1, Palette.CINNABAR);
            }

            if (isHovered && !dragHandler.isDragging()) {
                // 悬停时显示朱砂色高亮，如果是选中状态则叠加
                guiGraphics.renderOutline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, isSelected ? HIGHLIGHT_COLOR : Palette.CINNABAR);
                guiGraphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
            } else if (isSelected) {
                 // 仅仅选中状态的高亮
                 guiGraphics.renderOutline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, HIGHLIGHT_COLOR);
            }
        }

        // 拖拽悬停反馈
        if (isHovered && dragHandler.isDragging()) {
            guiGraphics.renderOutline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Palette.CINNABAR);
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
        int listSize = session.getInputItems().size();
        int startX = getX() + 15 - scrollOffset;
        int relativeX = (int) mouseX - startX;
        
        // 计算逻辑：根据相对于起点的位置，除以槽位宽+间距
        // 四舍五入到最近的索引位置
        int index = (relativeX + (SLOT_SIZE + GAP) / 2) / (SLOT_SIZE + GAP);
        // 限制最大索引不能超过当前最大容量
        int maxAllowed = session.getMaxInput();
        // 如果当前列表已满，且我们不是在替换（逻辑上这里只处理插入），则只能是在满的位置（无效）
        // 但为了简单，我们限制在 [0, listSize] 且 <= maxAllowed
        // 注意：如果 listSize == maxAllowed，那么 index 最大只能是 listSize-1 (替换) 或者禁止插入
        // 这里 session.insertInput 会做最终检查，但 UI 上不要显示越界的幽灵
        
        return Math.max(0, Math.min(index, Math.min(listSize, maxAllowed)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            // 滚轮控制左右滚动
            scrollOffset -= (int) (scrollY * 20);
            // 滚动时取消选中
            session.setSelectedIndex(-1);
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
        // 检查是否点击了滚动条区域 (判定区域扩大，方便点击)
        int listSize = session.getInputItems().size();
        int contentWidth = (listSize + 1) * (SLOT_SIZE + GAP) + 30;
        int maxScroll = Math.max(0, contentWidth - getWidth());
        
        // 判定区域：从底部向上 10 像素 (包含 SCROLLBAR_HEIGHT + 额外缓冲)
        if (maxScroll > 0 && mouseY >= getY() + getHeight() - 10 && mouseY <= getY() + getHeight()) {
             isScrolling = true;
             return true;
        }

        if (isMouseOver(mouseX, mouseY) && button == 0 && !dragHandler.isDragging()) {
            List<ItemStack> sequence = session.getInputItems();
            int centerY = getY() + getHeight() / 2;
            int startX = getX() + 15 - scrollOffset;

            for (int i = 0; i < sequence.size(); i++) {
                int slotX = startX + i * (SLOT_SIZE + GAP);
                int slotY = centerY - SLOT_SIZE / 2;

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    // 记录点击，准备拖拽或选中
                    pendingDragIndex = i;
                    clickX = mouseX;
                    clickY = mouseY;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling) {
            int listSize = session.getInputItems().size();
            int contentWidth = (listSize + 1) * (SLOT_SIZE + GAP) + 30;
            int maxScroll = Math.max(0, contentWidth - getWidth());
            int trackWidth = getWidth() - 10;
            
            if (maxScroll > 0 && trackWidth > 0) {
                // 计算滑块宽度
                float ratio = (float) getWidth() / contentWidth;
                int thumbWidth = Math.max(20, (int) (trackWidth * ratio));
                int availableTrack = trackWidth - thumbWidth;
                
                if (availableTrack > 0) {
                    // 鼠标每移动 1 像素，对应的滚动量
                    float scrollPerPixel = (float) maxScroll / availableTrack;
                    scrollOffset += (int) (dragX * scrollPerPixel);
                    scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
                }
            }
            return true;
        }
        
        // 延迟拖拽逻辑
        if (pendingDragIndex != -1 && !dragHandler.isDragging()) {
            double dist = Math.sqrt(Math.pow(mouseX - clickX, 2) + Math.pow(mouseY - clickY, 2));
            if (dist > DRAG_THRESHOLD) {
                List<ItemStack> sequence = session.getInputItems();
                if (pendingDragIndex < sequence.size()) {
                    dragHandler.startDrag(sequence.get(pendingDragIndex), pendingDragIndex);
                    session.removeInput(pendingDragIndex);
                    // 拖拽开始，清除选中状态
                    session.setSelectedIndex(-1);
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                pendingDragIndex = -1; // 重置
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false; // 停止滚动
        
        // 处理单击选中/反选
        if (pendingDragIndex != -1 && !dragHandler.isDragging()) {
            if (session.getSelectedIndex() == pendingDragIndex) {
                // 如果已选中，则取消选中
                session.setSelectedIndex(-1);
            } else {
                // 否则选中
                session.setSelectedIndex(pendingDragIndex);
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            pendingDragIndex = -1;
            return true;
        }
        pendingDragIndex = -1; // 确保重置

        if (isMouseOver(mouseX, mouseY) && dragHandler.isDragging()) {
            int index = calculateDropIndex(mouseX);
            ItemStack stack = dragHandler.getDraggingStack();
            
            session.insertInput(index, stack);
            dragHandler.endDrag();
            
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}