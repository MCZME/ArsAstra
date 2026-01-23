package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

@SuppressWarnings("null")
public abstract class AbstractTunBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 状态
    protected ResourceLocation fluidType = ResourceLocation.withDefaultNamespace("water");
    protected int fluidLevel = 0; // 0-3
    protected StarChartContext context;
    protected final StarChartEngine engine = new StarChartEngineImpl();

    // 搅拌状态
    protected ItemStack stirringStick = ItemStack.EMPTY;
    protected float stirProgress = 0.0f; // 0.0 - 1.0 (动画插值)
    protected boolean isStirring = false;
    protected boolean isStirringClockwise = true;

    // 性能优化状态
    protected boolean isHeated = false;
    protected int activeTimer = 0; // 活跃状态计时器 (ticks)
    
    // 客户端渲染状态 (不序列化)
    public float clientStirAnim = 0;

    public AbstractTunBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.context = new StarChartContext(Collections.emptyList(), StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap());
    }

    // --- Getter / Setter ---
    
    public ItemStack getStirringStick() { return stirringStick; }
    public void setStirringStick(ItemStack stick) { this.stirringStick = stick; }
    
    public float getStirProgress() { return stirProgress; }
    public void setStirProgress(float progress) { this.stirProgress = progress; }
    
    public boolean isStirring() { return isStirring; }
    public void setStirring(boolean stirring) { this.isStirring = stirring; }
    
    public boolean isStirringClockwise() { return isStirringClockwise; }
    public void setStirringClockwise(boolean clockwise) { this.isStirringClockwise = clockwise; }

    // --- 各等级特有属性的抽象方法 ---
    
    /**
     * @return 该等级釜的最大投入物品数量 (例如：铜釜为 8)
     */
    public abstract int getMaxInputCount();

    /**
     * @param fluid 流体类型 ID
     * @return 该流体是否可作为此釜的基底
     */
    public abstract boolean isFluidValid(ResourceLocation fluid);

    /**
     * @return 该釜的稳定性衰减系数 (1.0 = Tier 1, 越小衰减越少)
     */
    public abstract float getStabilityCoefficient();

    // --- 逻辑实现 ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractTunBlockEntity entity) {
        // 1. 每 20 ticks 检查一次热源，结果缓存到 isHeated
        if (level.getGameTime() % 20 == 0) {
            entity.isHeated = entity.checkHeat(level, pos.below());
        }
        
        // 搅拌动画逻辑 (不受热源影响，只要有操作就播放)
        if (entity.isStirring()) {
            float speed = 0.05f; // 20 tick (1秒) 完成
            entity.stirProgress += speed;
            if (entity.stirProgress >= 1.0f) {
                entity.stirProgress = 0.0f;
                entity.isStirring = false;
                entity.setChanged();
                entity.sync();
            }
        }

        // 无热源时不处理任何物品吸入
        if (!entity.isHeated) return;

        // 2. 自适应频率扫描物品
        // 策略:
        // - 活跃态 (activeTimer > 0): 每 2 ticks 扫描一次
        // - 空闲态 (activeTimer == 0): 每 20 ticks 扫描一次
        boolean shouldScan;
        if (entity.activeTimer > 0) {
            entity.activeTimer--;
            shouldScan = level.getGameTime() % 2 == 0;
        } else {
            shouldScan = level.getGameTime() % 20 == 0;
        }

        if (shouldScan) {
            if (entity.handleItemInput(level, pos)) {
                // 发现物品，进入/维持活跃态 (持续 100 ticks = 5秒)
                entity.activeTimer = 100;
            }
        }
    }

    protected boolean checkHeat(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    /**
     * 处理物品吸入逻辑
     * @return 如果在此次扫描中发现了任何有效的物品实体，返回 true
     */
    protected boolean handleItemInput(Level level, BlockPos pos) {
        if (this.fluidLevel <= 0) return false;

        // 捕获范围：釜内液体上方的空间
        AABB captureArea = new AABB(pos.getX() + 0.2, pos.getY() + 0.2, pos.getZ() + 0.2,
                                    pos.getX() + 0.8, pos.getY() + 1.0, pos.getZ() + 0.8);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, captureArea);
        if (items.isEmpty()) return false;

        boolean changed = false;
        boolean foundValidItem = false;

        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;

            // 只要发现活着的物品，就标记为发现有效物品，用于触发/维持活跃态
            foundValidItem = true;

            ItemStack stack = itemEntity.getItem();
            int count = stack.getCount();
            int currentInputCount = this.context.inputs().size();

            // 检查容量限制
            if (currentInputCount >= getMaxInputCount()) {
                continue; 
            }

            // 计算实际可吸收的数量
            int toTake = Math.min(count, getMaxInputCount() - currentInputCount);
            if (toTake <= 0) continue;

            List<AlchemyInput> newInputs = new ArrayList<>(this.context.inputs());
            for (int i = 0; i < toTake; i++) {
                newInputs.add(AlchemyInput.of(stack.copy().split(1)));
            }

            // 更新物品实体状态
            if (count == toTake) {
                itemEntity.discard();
            } else {
                stack.shrink(toTake);
                itemEntity.setItem(stack);
            }

            computeContext(newInputs);
            changed = true;
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.2f, 1.0f);
        }

        if (changed) {
            setChanged();
            sync();
        }
        
        return foundValidItem;
    }

    public InteractionResult onUse(Player player, InteractionHand hand) {
        return com.github.mczme.arsastra.block.interaction.TunInteractions.handleInteraction(this, player, hand);
    }
    
    public void setFluidLevel(int fluidLevel) {
        this.fluidLevel = fluidLevel;
    }

    public void setFluidType(ResourceLocation fluidType) {
        this.fluidType = fluidType;
    }

    public void resetContext() {
        this.context = new StarChartContext(Collections.emptyList(), StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap());
    }

    public void computeContext(List<AlchemyInput> inputs) {
        ResourceLocation chartId = fluidType.getPath().equals("water") 
                ? ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "base_chart") 
                : fluidType;

        if (StarChartManager.getInstance().getStarChart(chartId).isPresent()) {
            StarChart chart = StarChartManager.getInstance().getStarChart(chartId).get();
            this.context = engine.compute(chart, new StarChartContext(inputs, StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap()), getStabilityCoefficient());
        } else {
             this.context = new StarChartContext(inputs, StarChartRoute.EMPTY, Collections.emptyList(), 0.0f, Collections.emptyMap());
        }

        // 检查炸锅逻辑：稳定度归零触发爆炸
        if (this.context.stability() <= 0.0f && !inputs.isEmpty()) {
            triggerExplosion();
        }
    }

    protected void triggerExplosion() {
        if (this.level == null) return;
        
        BlockPos pos = this.worldPosition;
        // 1. 触发物理爆炸 (无方块破坏)
        this.level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.NONE);
        
        // 2. 清空状态
        this.fluidLevel = 0;
        resetContext();
        
        // 3. 同步
        this.setChanged();
        this.sync();
    }

    // --- 数据持久化 ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("FluidType", fluidType.toString());
        tag.putInt("FluidLevel", fluidLevel);
        
        if (!stirringStick.isEmpty()) {
            tag.put("StirringStick", stirringStick.save(registries));
        }
        tag.putFloat("StirProgress", stirProgress);
        tag.putBoolean("IsStirring", isStirring);
        tag.putBoolean("StirClockwise", isStirringClockwise);
        
        ListTag inputsTag = new ListTag();
        for (AlchemyInput input : context.inputs()) {
            AlchemyInput.CODEC.encodeStart(NbtOps.INSTANCE, input)
                    .resultOrPartial(ArsAstra.LOGGER::error)
                    .ifPresent(inputsTag::add);
        }
        tag.put("Inputs", inputsTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FluidType")) {
            this.fluidType = ResourceLocation.parse(tag.getString("FluidType"));
        }
        if (tag.contains("FluidLevel")) {
            this.fluidLevel = tag.getInt("FluidLevel");
        }
        
        if (tag.contains("StirringStick")) {
            this.stirringStick = ItemStack.parse(registries, tag.getCompound("StirringStick")).orElse(ItemStack.EMPTY);
        } else {
            this.stirringStick = ItemStack.EMPTY;
        }
        this.stirProgress = tag.getFloat("StirProgress");
        this.isStirring = tag.getBoolean("IsStirring");
        this.isStirringClockwise = tag.getBoolean("StirClockwise");
        
        List<AlchemyInput> loadedInputs = new ArrayList<>();
        if (tag.contains("Inputs", Tag.TAG_LIST)) {
            ListTag inputsTag = tag.getList("Inputs", Tag.TAG_COMPOUND);
            for (Tag t : inputsTag) {
                AlchemyInput.CODEC.parse(NbtOps.INSTANCE, t)
                        .resultOrPartial(ArsAstra.LOGGER::error)
                        .ifPresent(loadedInputs::add);
            }
        }
        computeContext(loadedInputs);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        loadAdditional(pkt.getTag(), lookupProvider);
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- Getter 方法与客户端逻辑 ---

    public StarChartContext getContext() { return context; }
    public int getFluidLevel() { return fluidLevel; }
    public ResourceLocation getFluidType() { return fluidType; }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AbstractTunBlockEntity entity) {
        if (entity.isStirring) {
            entity.clientStirAnim += 1.0f;
        } else {
            entity.clientStirAnim = 0;
        }
        
        if (entity.fluidLevel <= 0) return;

        float stability = entity.context.stability();
        float random = level.random.nextFloat();

        // 计算液面高度
        double surfaceY = pos.getY() + 0.2 + (entity.fluidLevel * 0.25);

        // 基础循环：根据稳定度决定粒子密度
        // 稳定度越低，粒子产生的概率越高 (0.02 - 0.3)
        float particleChance = 0.02f + (1.0f - stability) * 0.28f;

        if (random < particleChance) {
            double px = pos.getX() + 0.2 + level.random.nextDouble() * 0.6;
            double pz = pos.getZ() + 0.2 + level.random.nextDouble() * 0.6;

            if (stability > 0.8f) {
                // 高稳定：金色微光 (极其稀疏)
                level.addParticle(ParticleTypes.END_ROD, px, surfaceY, pz, 0, 0.005, 0);
            } else if (stability > 0.4f) {
                // 中稳定：正常气泡
                level.addParticle(ParticleTypes.BUBBLE, px, surfaceY, pz, 0, 0.01, 0);
            } else if (stability > 0.2f) {
                // 低稳定：飞溅水花
                level.addParticle(ParticleTypes.SPLASH, px, surfaceY, pz, 0, 0.1, 0);
            } else {
                // 临界：黑烟与愤怒粒子
                level.addParticle(ParticleTypes.SMOKE, px, surfaceY, pz, 0, 0.05, 0);
                if (level.random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.ANGRY_VILLAGER, px, surfaceY + 0.2, pz, 0, 0, 0);
                }
            }
        }

        // 听觉反馈：随着稳定度降低，播放音效的频率增加
        // 高稳定 (>0.8): 80 tick (4s)
        // 临界 (0.0): 10 tick (0.5s)
        int soundInterval = (int) (10 + stability * 70);
        if (level.getGameTime() % soundInterval == 0) {
             float pitch = 0.8f + (1.0f - stability) * 0.5f; // 稳定度越低，音调越高 (0.8 ~ 1.3)
             level.playLocalSound(pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.15f, pitch, false);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
