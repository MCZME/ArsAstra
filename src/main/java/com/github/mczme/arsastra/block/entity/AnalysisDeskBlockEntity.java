package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.network.AANetwork;
import com.github.mczme.arsastra.registry.AAAttachments;
import com.github.mczme.arsastra.registry.AABlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnalysisDeskBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return !isLocked; // 锁定时禁止放入或取出
        }
    };

    private boolean isLocked = false;
    private int guessesRemaining = 5;
    private boolean isResearching = false;
    private final Map<ResourceLocation, Integer> lastFeedback = new HashMap<>(); // -1: Low, 0: Match, 1: High

    // 学者路线消耗的等级
    private static final int SCHOLAR_XP_COST_LEVELS = 5;

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
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, pRegistries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putBoolean("isLocked", isLocked);
        pTag.putInt("guessesRemaining", guessesRemaining);
        pTag.putBoolean("isResearching", isResearching);

        ListTag feedbackTag = new ListTag();
        lastFeedback.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", key.toString());
            entry.putInt("val", val);
            feedbackTag.add(entry);
        });
        pTag.put("lastFeedback", feedbackTag);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        isLocked = pTag.getBoolean("isLocked");
        guessesRemaining = pTag.getInt("guessesRemaining");
        isResearching = pTag.getBoolean("isResearching");

        lastFeedback.clear();
        if (pTag.contains("lastFeedback", Tag.TAG_LIST)) {
            ListTag list = pTag.getList("lastFeedback", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag entry = (CompoundTag) t;
                lastFeedback.put(ResourceLocation.parse(entry.getString("id")), entry.getInt("val"));
            }
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public int getGuessesRemaining() {
        return guessesRemaining;
    }

    public boolean isResearching() {
        return isResearching;
    }
    
    public Map<ResourceLocation, Integer> getLastFeedback() {
        return lastFeedback;
    }

    // ================== 业务逻辑 ==================

    public void serverPerformDirectAnalysis(Player player) {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.hasAnalyzed(stack.getItem())) {
            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.already_known"), true);
            return;
        }

        if (player.experienceLevel < SCHOLAR_XP_COST_LEVELS && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.not_enough_xp"), true);
            return;
        }

        if (!player.isCreative()) {
            player.giveExperienceLevels(-SCHOLAR_XP_COST_LEVELS);
        }

        knowledge.analyzeItem(stack.getItem());
        if (player instanceof ServerPlayer sp) {
            AANetwork.sendToPlayer(sp);
        }

        player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.success"), true);
        resetResearch();
    }

    public void serverStartIntuitionAnalysis(Player player) {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        Optional<ElementProfile> profile = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
        if (profile.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.no_elements"), true);
            return;
        }

        this.isLocked = true;
        this.isResearching = true;
        this.guessesRemaining = 5;
        this.lastFeedback.clear();
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void serverSubmitGuess(Player player, Map<ResourceLocation, Integer> guesses) {
        if (level == null || level.isClientSide) return;
        if (!isResearching || !isLocked) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        Optional<ElementProfile> profileOpt = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
        if (profileOpt.isEmpty()) {
            resetResearch();
            return;
        }
        ElementProfile profile = profileOpt.get();
        Map<ResourceLocation, Float> actualElements = profile.elements();

        boolean allCorrect = true;
        this.lastFeedback.clear();

        for (Map.Entry<ResourceLocation, Float> entry : actualElements.entrySet()) {
            ResourceLocation elementId = entry.getKey();
            float actualVal = entry.getValue();
            
            Integer guessVal = guesses.get(elementId);
            if (guessVal == null) {
                guessVal = 0; // Treat missing guess as 0
            }

            int diff = 0;
            // 容差范围
            if (guessVal < actualVal - 5) diff = -1; // 猜小了
            else if (guessVal > actualVal + 5) diff = 1; // 猜大了
            
            this.lastFeedback.put(elementId, diff);
            
            if (diff != 0) allCorrect = false;
        }

        if (allCorrect) {
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            knowledge.analyzeItem(stack.getItem());
            if (player instanceof ServerPlayer sp) {
                AANetwork.sendToPlayer(sp);
            }

            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.success_intuition"), true);
            player.giveExperiencePoints(50);
            resetResearch();
        } else {
            this.guessesRemaining--;
            if (this.guessesRemaining <= 0) {
                itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.failure_destroyed"), true);
                resetResearch();
            } else {
                player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.guess_wrong", guessesRemaining), true);
            }
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void serverQuitGuess(Player player) {
        // 作为"安全逃生"，直接转为直接分析
        if (isResearching) {
            // 注意：这里我们允许从中途转为直接分析，逻辑同 DirectAnalysis，但无需检查是否已分析（因为肯定未分析）
             if (player.experienceLevel < SCHOLAR_XP_COST_LEVELS && !player.isCreative()) {
                player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.not_enough_xp"), true);
                return;
            }

            if (!player.isCreative()) {
                player.giveExperienceLevels(-SCHOLAR_XP_COST_LEVELS);
            }
            
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!stack.isEmpty()) {
                PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
                knowledge.analyzeItem(stack.getItem());
                if (player instanceof ServerPlayer sp) {
                    AANetwork.sendToPlayer(sp);
                }
            }
            
            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.success"), true);
            resetResearch();
        }
    }

    private void resetResearch() {
        this.isLocked = false;
        this.isResearching = false;
        this.guessesRemaining = 5;
        this.lastFeedback.clear();
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
