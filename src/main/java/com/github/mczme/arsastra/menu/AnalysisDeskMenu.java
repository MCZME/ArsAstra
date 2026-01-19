package com.github.mczme.arsastra.menu;

import com.github.mczme.arsastra.block.entity.AnalysisDeskBlockEntity;
import com.github.mczme.arsastra.registry.AABlocks;
import com.github.mczme.arsastra.registry.AAMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AnalysisDeskMenu extends AbstractContainerMenu {
    public final AnalysisDeskBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    public AnalysisDeskMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AnalysisDeskMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(AAMenus.ANALYSIS_DESK.get(), pContainerId);
        blockEntity = (AnalysisDeskBlockEntity) entity;
        this.levelAccess = ContainerLevelAccess.create(inv.player.level(), entity.getBlockPos());

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 13, 20));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index == slots.size() - 1) { // 物品槽
                if (!moveItemStackTo(stack, 0, slots.size() - 1, true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, slots.size() - 1, slots.size(), false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, AABlocks.ANALYSIS_DESK.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 背包 Y 坐标：121 (标准背包起始位置通常比 GUI 高度少 82-84 像素)
        // 这里基于总高度 202，假设底部留 7px 边距，Hotbar 在 179，则背包在 121
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 121 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // 快捷栏 Y 坐标：179
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 179));
        }
    }
}
