package com.github.mczme.arsastra.menu;

import com.github.mczme.arsastra.registry.AAMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StarChartJournalMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;

    public StarChartJournalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public StarChartJournalMenu(int containerId, Inventory playerInventory) {
        super(AAMenus.STAR_CHART_JOURNAL_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        layoutPlayerInventorySlots(8, 174); // 假设 GUI 高度较大，调整下方位置
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // TODO: Implement shift-click logic
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 虚拟菜单，总是有效，或者检查手持物品
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftCol + col * 18, topRow + 58));
        }
    }
}
