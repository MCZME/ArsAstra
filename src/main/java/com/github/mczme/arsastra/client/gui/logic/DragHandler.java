package com.github.mczme.arsastra.client.gui.logic;

import net.minecraft.world.item.ItemStack;

public interface DragHandler {
    void startDrag(ItemStack stack);
    boolean isDragging();
    ItemStack getDraggingStack();
    void endDrag();
}
