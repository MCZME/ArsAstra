package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.registry.AABlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AnalysisDeskBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return !isLocked; // 锁定时禁止放入或取出
        }
    };

    private boolean isLocked = false;
    private int guessesRemaining = 5;
    private int targetStrength = 0;
    private boolean isResearching = false;

    public AnalysisDeskBlockEntity(BlockPos pos, BlockState blockState) {
        super(AABlockEntities.ANALYSIS_DESK.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ars_astra.analysis_desk");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new AnalysisDeskMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putBoolean("isLocked", isLocked);
        pTag.putInt("guessesRemaining", guessesRemaining);
        pTag.putInt("targetStrength", targetStrength);
        pTag.putBoolean("isResearching", isResearching);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        isLocked = pTag.getBoolean("isLocked");
        guessesRemaining = pTag.getInt("guessesRemaining");
        targetStrength = pTag.getInt("targetStrength");
        isResearching = pTag.getBoolean("isResearching");
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
        setChanged();
    }

    public int getGuessesRemaining() {
        return guessesRemaining;
    }

    public void setGuessesRemaining(int guessesRemaining) {
        this.guessesRemaining = guessesRemaining;
        setChanged();
    }

    public boolean isResearching() {
        return isResearching;
    }

    public void setResearching(boolean researching) {
        isResearching = researching;
        setChanged();
    }
}
