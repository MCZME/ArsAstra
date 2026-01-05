package com.github.mczme.arsastra.menu;

import com.github.mczme.arsastra.registry.AAMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class StarChartJournalMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;

    public StarChartJournalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public StarChartJournalMenu(int containerId, Inventory playerInventory) {
        super(AAMenus.STAR_CHART_JOURNAL_MENU.get(), containerId);
        this.playerInventory = playerInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 虚拟菜单，总是有效，或者检查手持物品
    }
}
