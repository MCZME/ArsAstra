package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopSession;
import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.client.gui.widget.StarChartWidget;
import com.github.mczme.arsastra.client.gui.widget.toolbar.ToolbarTabButton;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class WorkshopCanvasWidget extends StarChartWidget {
    private final DragHandler dragHandler;
    private final WorkshopSession session;
    
    private final ToolbarTabButton btnRotateCCW;
    private final ToolbarTabButton btnRotateCW;

    // 垃圾桶区域定义
    private static final int TRASH_WIDTH = 30;
    private static final int TRASH_HEIGHT = 40;

    public WorkshopCanvasWidget(int x, int y, int width, int height, DragHandler dragHandler, WorkshopSession session) {
        super(x, y, width, height, Component.empty());
        this.dragHandler = dragHandler;
        this.session = session;
        
        // 初始化旋转按钮
        // 逆时针 (-5度)
        this.btnRotateCCW = new ToolbarTabButton(0, 0, 24, 20, Component.empty(), 12, Palette.CINNABAR, () -> {
            session.stirInput(session.getSelectedIndex(), session.getSelectedInput().rotation() - (float)Math.toRadians(5));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        });
        this.btnRotateCCW.setDirection(ToolbarTabButton.Direction.RIGHT);
        
        // 顺时针 (+5度)
        this.btnRotateCW = new ToolbarTabButton(0, 0, 24, 20, Component.empty(), 11, Palette.CINNABAR, () -> {
            session.stirInput(session.getSelectedIndex(), session.getSelectedInput().rotation() + (float)Math.toRadians(5));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        });
        this.btnRotateCW.setDirection(ToolbarTabButton.Direction.RIGHT);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 从 Session 同步最新的推演结果
        if (this.session != null) {
            this.setDeductionResult(this.session.getDeductionResult());
        }
        
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (dragHandler.isDragging()) {
            // 拖拽中：显示垃圾桶
            renderTrashBin(guiGraphics, mouseX, mouseY);
        } else {
            // 未拖拽：如果选中了物品，显示旋转按钮
            AlchemyInput selected = session.getSelectedInput();
            if (selected != null) {
                // 更新按钮位置 (贴右边缘外部，留2px重叠)
                int btnX = getX() + getWidth() - 2;
                int centerY = getY() + getHeight() / 2;
                
                this.btnRotateCCW.setX(btnX);
                this.btnRotateCCW.setY(centerY - 22); // 上方
                this.btnRotateCCW.render(guiGraphics, mouseX, mouseY, partialTick);
                
                this.btnRotateCW.setX(btnX);
                this.btnRotateCW.setY(centerY + 2); // 下方
                this.btnRotateCW.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * 在屏幕右侧绘制垃圾桶
     */
    private void renderTrashBin(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int tx = getX() + getWidth() - TRASH_WIDTH - 10;
        int ty = getY() + (getHeight() - TRASH_HEIGHT) / 2;
        
        boolean isHovered = mouseX >= tx && mouseX < tx + TRASH_WIDTH && mouseY >= ty && mouseY < ty + TRASH_HEIGHT;
        
        // 垃圾桶背景
        int color = isHovered ? Palette.CINNABAR : Palette.INK;
        guiGraphics.fill(tx, ty, tx + TRASH_WIDTH, ty + TRASH_HEIGHT, Palette.PARCHMENT_BG);
        guiGraphics.renderOutline(tx, ty, TRASH_WIDTH, TRASH_HEIGHT, color);
        
        // 绘制简单的垃圾桶图标 (手绘感)
        int iconX = tx + 7;
        int iconY = ty + 10;
        guiGraphics.fill(iconX, iconY, iconX + 16, iconY + 2, color); // 盖子
        guiGraphics.fill(iconX + 2, iconY + 3, iconX + 14, iconY + 20, color & 0x40FFFFFF); // 桶身 (半透明)
        guiGraphics.renderOutline(iconX + 2, iconY + 3, 12, 17, color); // 桶身轮廓
        
        // 绘制竖线
        guiGraphics.fill(iconX + 5, iconY + 6, iconX + 6, iconY + 16, color);
        guiGraphics.fill(iconX + 8, iconY + 6, iconX + 9, iconY + 16, color);
        guiGraphics.fill(iconX + 11, iconY + 6, iconX + 12, iconY + 16, color);

        if (isHovered) {
            // 显示删除提示
            guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("gui.ars_astra.workshop.remove"), mouseX, mouseY);
        }
    }

    private boolean isOverTrash(double mouseX, double mouseY) {
        int tx = getX() + getWidth() - TRASH_WIDTH - 10;
        int ty = getY() + (getHeight() - TRASH_HEIGHT) / 2;
        return mouseX >= tx && mouseX < tx + TRASH_WIDTH && mouseY >= ty && mouseY < ty + TRASH_HEIGHT;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!dragHandler.isDragging() && session.getSelectedInput() != null) {
            if (btnRotateCCW.mouseClicked(mouseX, mouseY, button)) return true;
            if (btnRotateCW.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragHandler.isDragging()) {
            // 检查是否丢入垃圾桶
            if (isOverTrash(mouseX, mouseY)) {
                // 如果是序列中拖出来的，已经在 dragStart 时 remove 了，这里直接结束即可实现删除
                // 如果是新拖入的，endDrag 即可取消添加
                dragHandler.endDrag();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK, 1.0F));
                return true;
            }

            // 正常的画布放置逻辑
            if (isMouseOver(mouseX, mouseY)) {
                session.addInput(dragHandler.getDraggingStack());
                dragHandler.endDrag();
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
