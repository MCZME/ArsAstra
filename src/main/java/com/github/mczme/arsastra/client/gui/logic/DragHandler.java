package com.github.mczme.arsastra.client.gui.logic;

import net.minecraft.world.item.ItemStack;

/**
 * 拖拽处理器接口，用于管理推演工坊中的物品拖拽操作。
 */
public interface DragHandler {
    /**
     * 开始拖拽物品。
     * @param stack 正在拖拽的物品堆栈。
     * @param sourceIndex 物品在序列中的索引，如果是新物品（如来自原料面板）则为 -1。
     */
    void startDrag(ItemStack stack, int sourceIndex);

    /**
     * 开始拖拽一个新物品（默认索引为 -1）。
     */
    default void startDrag(ItemStack stack) {
        startDrag(stack, -1);
    }

    /**
     * 是否正在拖拽中。
     */
    boolean isDragging();

    /**
     * 获取当前正在拖拽的物品堆栈。
     */
    ItemStack getDraggingStack();
    
    /**
     * 获取拖拽物品的来源索引。
     * @return 序列索引，-1 表示新物品。
     */
    int getDragSourceIndex();

    /**
     * 结束拖拽操作。
     */
    void endDrag();
}