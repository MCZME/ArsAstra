package com.github.mczme.arsastra.client.gui.widget.workshop;

import com.github.mczme.arsastra.client.gui.logic.DragHandler;
import com.github.mczme.arsastra.client.gui.logic.WorkshopViewModel;
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
    private static final int GAP = 10; // Increased gap for timeline look

    public SequenceStripWidget(int x, int y, int width, WorkshopViewModel viewModel, DragHandler dragHandler) {
        super(x, y, width, 40, Component.translatable("gui.ars_astra.workshop.sequence")); // Increased height
        this.viewModel = viewModel;
        this.dragHandler = dragHandler;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Background (Parchment style)
        int bgColor = 0xFFE3D8B4;
        int inkColor = 0xFF2F2F2F;
        int highlightColor = 0xFF8B0000; // Cinnabar

        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), inkColor);

        // 2. Timeline Line
        int centerY = getY() + getHeight() / 2;
        guiGraphics.fill(getX() + 5, centerY, getX() + getWidth() - 5, centerY + 1, 0x802F2F2F);

        List<ItemStack> sequence = viewModel.getSequence();
        int startX = getX() + 15 - scrollOffset;

        // Enable scissor to clip scrolling items
        guiGraphics.enableScissor(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2);

        for (int i = 0; i <= sequence.size(); i++) {
            int slotX = startX + i * (SLOT_SIZE + GAP);
            int slotY = centerY - SLOT_SIZE / 2;

            if (i < sequence.size()) {
                // Existing Item Node
                // Draw node background (circle-ish)
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor); // Clear line behind item
                guiGraphics.renderOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE, inkColor);

                ItemStack stack = sequence.get(i);
                guiGraphics.renderFakeItem(stack, slotX + 2, slotY + 2);

                // Hover Highlight & Tooltip
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    guiGraphics.renderOutline(slotX - 1, slotY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, highlightColor);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                }
            } else {
                // "Next Slot" Placeholder (Dashed/Faint)
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor); // Clear line
                // Simple faint box for now, could be dashed
                guiGraphics.renderOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE, 0x602F2F2F);
                
                // If dragging over this empty slot, highlight it
                if (dragHandler.isDragging() && mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                     guiGraphics.renderOutline(slotX - 1, slotY - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, highlightColor);
                }
            }
        }
        
        guiGraphics.disableScissor();
    }
    
    @Override
    protected boolean isMouseOverTitle(double mouseX, double mouseY) {
        return false; // Strip is not draggable by title
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
                    // Play sound
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
            // Calculate drop index
            int startX = getX() + 15 - scrollOffset;
            int relativeX = (int)mouseX - startX;
            // index * (SIZE + GAP) + SIZE/2 approx
            int index = Math.max(0, (relativeX + (SLOT_SIZE + GAP) / 2) / (SLOT_SIZE + GAP));
            
            // Clamp index
            int maxIndex = viewModel.getSequence().size();
            index = Math.min(index, maxIndex);
            
            viewModel.insertToSequence(index, dragHandler.getDraggingStack());
            dragHandler.endDrag();
            // Play sound
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
