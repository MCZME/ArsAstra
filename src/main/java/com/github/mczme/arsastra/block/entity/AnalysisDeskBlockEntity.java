package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.network.AANetwork;
import com.github.mczme.arsastra.network.payload.AnalysisActionPayload;
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
    
    // 直觉路线配置
    private static final int TOLERANCE_PRECISE = 5;
    private static final int TOLERANCE_RANGE = 15;
    private static final int XP_REWARD_BASE = 50;
    private static final float XP_MULTIPLIER_PRECISE = 1.5f;
    private static final float XP_MULTIPLIER_RANGE = 0.5f;

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

    public void serverSubmitGuess(Player player, Map<ResourceLocation, AnalysisActionPayload.GuessData> guesses) {
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
        
        float totalXpMultiplier = 0.0f;
        int elementCount = actualElements.size();

        for (Map.Entry<ResourceLocation, Float> entry : actualElements.entrySet()) {
            ResourceLocation elementId = entry.getKey();
            float actualVal = entry.getValue();
            
            AnalysisActionPayload.GuessData guessData = guesses.get(elementId);
            int guessVal = (guessData != null) ? guessData.value() : 0;
            boolean isPrecise = (guessData != null) && guessData.isPrecise();
            
            int tolerance = isPrecise ? TOLERANCE_PRECISE : TOLERANCE_RANGE;
            int diff = 0;
            
            if (guessVal < actualVal - tolerance) diff = -1; // 猜小了
            else if (guessVal > actualVal + tolerance) diff = 1; // 猜大了
            
            this.lastFeedback.put(elementId, diff);
            
            if (diff != 0) {
                allCorrect = false;
            } else {
                totalXpMultiplier += isPrecise ? XP_MULTIPLIER_PRECISE : XP_MULTIPLIER_RANGE;
            }
        }

        if (allCorrect) {
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            knowledge.analyzeItem(stack.getItem());
            if (player instanceof ServerPlayer sp) {
                AANetwork.sendToPlayer(sp);
            }

            player.displayClientMessage(Component.translatable("gui.ars_astra.analysis.success_intuition"), true);
            
            float avgMult = totalXpMultiplier / Math.max(1, elementCount);
            int reward = Math.round(XP_REWARD_BASE * avgMult * 2);
            
            player.giveExperiencePoints(Math.max(10, reward));
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
        if (isResearching) {
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