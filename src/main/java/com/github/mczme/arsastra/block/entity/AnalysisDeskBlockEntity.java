package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import com.github.mczme.arsastra.network.AANetwork;
import com.github.mczme.arsastra.network.payload.AnalysisActionPayload;
import com.github.mczme.arsastra.network.payload.AnalysisResultPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("null")
public class AnalysisDeskBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(slot).isEmpty() && isLocked) {
                resetResearch();
            }
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return !isLocked; // 锁定时禁止放入
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isLocked) {
                return ItemStack.EMPTY; // 锁定时禁止取出
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 一次只能输入一个物品
        }
    };

    private boolean isLocked = false;
    private int guessesRemaining = 5;
    private boolean isResearching = false;
    private final Map<ResourceLocation, Integer> lastFeedback = new HashMap<>(); // -1: Low, 0: Match, 1: High
    
    // 动态会话数据
    private final Map<ResourceLocation, Integer> currentScales = new HashMap<>();
    private final Map<ResourceLocation, Integer> currentTolerances = new HashMap<>();

    // 经验经济配置
    private static final int BASE_ANALYSIS_COST = 150;
    private static final float STRENGTH_COST_FACTOR = 2.0f;
    
    // 直觉路线配置
    private static final int TOLERANCE_PRECISE = 5;
    private static final int TOLERANCE_RANGE = 15;
    private static final float REWARD_FACTOR = 1.5f;
    private static final float XP_MULTIPLIER_PRECISE = 1.2f;
    private static final float XP_MULTIPLIER_RANGE = 0.6f;

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

        ListTag scalesTag = new ListTag();
        currentScales.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", key.toString());
            entry.putInt("val", val);
            scalesTag.add(entry);
        });
        pTag.put("currentScales", scalesTag);

        ListTag tolerancesTag = new ListTag();
        currentTolerances.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", key.toString());
            entry.putInt("val", val);
            tolerancesTag.add(entry);
        });
        pTag.put("currentTolerances", tolerancesTag);
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
        
        currentScales.clear();
        if (pTag.contains("currentScales", Tag.TAG_LIST)) {
            ListTag list = pTag.getList("currentScales", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag entry = (CompoundTag) t;
                currentScales.put(ResourceLocation.parse(entry.getString("id")), entry.getInt("val"));
            }
        }

        currentTolerances.clear();
        if (pTag.contains("currentTolerances", Tag.TAG_LIST)) {
            ListTag list = pTag.getList("currentTolerances", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag entry = (CompoundTag) t;
                currentTolerances.put(ResourceLocation.parse(entry.getString("id")), entry.getInt("val"));
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

    public Map<ResourceLocation, Integer> getCurrentScales() {
        return currentScales;
    }

    public Map<ResourceLocation, Integer> getCurrentTolerances() {
        return currentTolerances;
    }

    // ================== 业务逻辑 ==================

    public void serverPerformDirectAnalysis(Player player) {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.hasAnalyzed(stack.getItem())) {
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.already_known"), true));
            }
            return;
        }

        Optional<ElementProfile> profileOpt = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
        if (profileOpt.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.no_elements"), true));
            }
            return;
        }
        ElementProfile profile = profileOpt.get();

        int totalStrength = calculateTotalStrength(profile);
        int cost = calculateAnalysisCost(totalStrength);

        if (player.totalExperience < cost && !player.isCreative()) {
            if (player instanceof ServerPlayer sp) {
                // 暂时使用通用提示，虽然最好能显示具体数值
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.not_enough_xp"), true));
            }
            return;
        }

        if (!player.isCreative()) {
            player.giveExperiencePoints(-cost);
        }

        knowledge.analyzeItem(stack.getItem());
        if (player instanceof ServerPlayer sp) {
            AANetwork.sendToPlayer(sp);
            PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.success"), false));
        }
        
        resetResearch();
    }

    public void serverStartIntuitionAnalysis(Player player) {
        if (level == null || level.isClientSide) return;

        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
        if (knowledge.hasAnalyzed(stack.getItem())) {
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.already_known"), true));
            }
            return;
        }

        Optional<ElementProfile> profile = ElementProfileManager.getInstance().getElementProfile(stack.getItem());
        if (profile.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.no_elements"), true));
            }
            return;
        }

        this.isLocked = true;
        this.isResearching = true;
        this.guessesRemaining = 5;
        this.lastFeedback.clear();
        this.currentScales.clear();
        this.currentTolerances.clear();
        
        ElementProfile profileObj = profile.get();
        for (Map.Entry<ResourceLocation, Float> entry : profileObj.elements().entrySet()) {
            float actual = entry.getValue();
            
            // 动态标尺生成
            // Scale Max = actual * (1.2 ~ 2.2) + (10 ~ 40)
            float randomScaleMult = 1.2f + level.random.nextFloat();
            float randomOffset = 10.0f + level.random.nextFloat() * 30.0f;
            int scaleMax = Math.round(actual * randomScaleMult + randomOffset);
            this.currentScales.put(entry.getKey(), scaleMax);
            
            // 动态难度/宽容度生成
            // Difficulty = (0.5 ~ 2.0) + (actual / 100) * (0 ~ 1.0)
            // 强度越大，难度系数上限越高
            float difficulty = 0.5f + level.random.nextFloat() * 1.5f;
            difficulty += (actual / 100.0f) * level.random.nextFloat();
            
            // Base Tolerance = 10
            // Tolerance = 10 / Difficulty
            int rawTolerance = Math.round(10.0f / difficulty);
            
            // 限制 Tolerance 不超过 Scale Max 的 10% (精确模式下)
            int maxAllowedTolerance = Math.max(1, (int)(scaleMax * 0.1f));
            int tolerance = Math.max(1, Math.min(rawTolerance, maxAllowedTolerance));
            
            this.currentTolerances.put(entry.getKey(), tolerance);
        }

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
            
            // 获取动态宽容度，默认为 5 (防空指针)
            int baseTolerance = currentTolerances.getOrDefault(elementId, 5);
            // 模糊模式宽容度放大 2 倍
            int tolerance = isPrecise ? baseTolerance : baseTolerance * 2;
            
            int diff = 0;
            
            if (guessVal < actualVal - tolerance) diff = -1; // 猜小了
            else if (guessVal > actualVal + tolerance) diff = 1; // 猜大了
            
            this.lastFeedback.put(elementId, diff);
            
            if (diff != 0) {
                allCorrect = false;
            } else {
                // 动态计算奖励倍率
                float difficultyMult = 10.0f / Math.max(1, baseTolerance);
                totalXpMultiplier += isPrecise ? (XP_MULTIPLIER_PRECISE * difficultyMult) : (XP_MULTIPLIER_RANGE * difficultyMult);
            }
        }

        if (allCorrect) {
            PlayerKnowledge knowledge = player.getData(AAAttachments.PLAYER_KNOWLEDGE);
            knowledge.analyzeItem(stack.getItem());
            if (player instanceof ServerPlayer sp) {
                AANetwork.sendToPlayer(sp);
                PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.success_intuition"), false));
            }
            
            int totalStrength = calculateTotalStrength(profile);
            float avgMult = totalXpMultiplier / Math.max(1, elementCount);
            int reward = Math.round((totalStrength * REWARD_FACTOR) * avgMult);
            
            player.giveExperiencePoints(Math.max(10, reward));
            resetResearch();
        } else {
            this.guessesRemaining--;
            if (this.guessesRemaining <= 0) {
                itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.failure_destroyed"), true));
                }
                resetResearch();
            } else {
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new AnalysisResultPayload(Component.translatable("gui.ars_astra.analysis.guess_wrong", guessesRemaining), true));
                }
            }
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

    private int calculateTotalStrength(ElementProfile profile) {
        float total = 0;
        for (float val : profile.elements().values()) {
            total += val;
        }
        return Math.round(total);
    }

    private int calculateAnalysisCost(int totalStrength) {
        return Math.round(BASE_ANALYSIS_COST + (totalStrength * STRENGTH_COST_FACTOR));
    }
}