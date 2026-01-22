package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import com.github.mczme.arsastra.core.starchart.engine.StarChartContext;
import com.github.mczme.arsastra.core.starchart.engine.StarChartEngine;
import com.github.mczme.arsastra.core.starchart.engine.StarChartEngineImpl;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.registry.AABlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("null")
public class CopperTunBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.copper_tun.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Star Chart State
    private ResourceLocation fluidType = ResourceLocation.withDefaultNamespace("water"); // Default to water
    private int fluidLevel = 0; // 0-3
    private StarChartContext context;
    private final StarChartEngine engine = new StarChartEngineImpl();

    public CopperTunBlockEntity(BlockPos pos, BlockState state) {
        super(AABlockEntities.COPPER_TUN.get(), pos, state);
        this.context = new StarChartContext(Collections.emptyList(), StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CopperTunBlockEntity entity) {
        // 1. 热源检查（MVP 阶段：每个 tick 都检查）
        if (checkHeat(level, pos.below())) {
            handleItemInput(level, pos, entity);
        }
    }

    public InteractionResult onUse(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        
        // 1. Fill with Water Bucket
        if (heldItem.getItem() == Items.WATER_BUCKET && fluidLevel < 3) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, new ItemStack(Items.BUCKET)));
            this.fluidLevel = 3;
            this.fluidType = ResourceLocation.withDefaultNamespace("water");
            this.context = new StarChartContext(Collections.emptyList(), StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap());
            this.setChanged();
            this.sync();
            this.level.playSound(null, this.worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // 2. Empty with Bucket
        if (heldItem.getItem() == Items.BUCKET && fluidLevel > 0) {
             // For MVP, assuming it's always water or compatible
            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, new ItemStack(Items.WATER_BUCKET)));
            this.fluidLevel = 0;
            this.context = new StarChartContext(Collections.emptyList(), StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap());
            this.setChanged();
            this.sync();
            this.level.playSound(null, this.worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // 3. Extract with Bottle
        if (heldItem.getItem() == Items.GLASS_BOTTLE && fluidLevel > 0) {
            ItemStack potionStack = new ItemStack(Items.POTION);
            
            // Build Potion Contents
            List<MobEffectInstance> effects = new ArrayList<>();
            for (Map.Entry<com.github.mczme.arsastra.core.starchart.EffectField, PotionData> entry : context.predictedEffects().entrySet()) {
                 com.github.mczme.arsastra.core.starchart.EffectField field = entry.getKey();
                 PotionData data = entry.getValue();
                 var effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(field.effect());
                 effectHolder.ifPresent(holder -> 
                     effects.add(new MobEffectInstance(holder, data.duration(), data.level()))
                 );
            }

            potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(PotionContents.getColor(effects)), effects));
            
            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, potionStack));
            
            this.fluidLevel--;
            this.setChanged();
            this.sync();
            this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static boolean checkHeat(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    private static void handleItemInput(Level level, BlockPos pos, CopperTunBlockEntity entity) {
        if (entity.fluidLevel <= 0) return; // No fluid, no processing

        // 捕获范围：釜内空间
        AABB captureArea = new AABB(pos.getX() + 0.2, pos.getY() + 0.2, pos.getZ() + 0.2,
                                    pos.getX() + 0.8, pos.getY() + 1.0, pos.getZ() + 0.8);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, captureArea);

        boolean changed = false;
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;

            ItemStack stack = itemEntity.getItem();
            int count = stack.getCount();

            ArsAstra.LOGGER.info("铜釜吸入了 {} x {}", count, stack.getHoverName().getString());

            // Add inputs
            List<AlchemyInput> newInputs = new ArrayList<>(entity.context.inputs());
            for (int i = 0; i < count; i++) {
                newInputs.add(AlchemyInput.of(stack.copy().split(1)));
            }
            
            // Recompute context
            entity.computeContext(newInputs);
            changed = true;

            // 视觉与音频反馈
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.2f, 1.0f);
            
            // 消耗物品
            itemEntity.discard();
        }
        
        if (changed) {
            entity.setChanged();
            entity.sync();
        }
    }

    private void computeContext(List<AlchemyInput> inputs) {
        Optional<StarChart> chartOpt = StarChartManager.getInstance().getStarChart(fluidType); 
        ResourceLocation chartId = fluidType.getPath().equals("water") 
                ? ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "base_chart") 
                : fluidType;

        if (StarChartManager.getInstance().getStarChart(chartId).isPresent()) {
            StarChart chart = StarChartManager.getInstance().getStarChart(chartId).get();
            this.context = engine.compute(chart, new StarChartContext(inputs, StarChartRoute.EMPTY, Collections.emptyList(), 1.0f, Collections.emptyMap()));
        } else {
             this.context = new StarChartContext(inputs, StarChartRoute.EMPTY, Collections.emptyList(), 0.0f, Collections.emptyMap());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("FluidType", fluidType.toString());
        tag.putInt("FluidLevel", fluidLevel);
        
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
        
        List<AlchemyInput> loadedInputs = new ArrayList<>();
        if (tag.contains("Inputs", Tag.TAG_LIST)) {
            ListTag inputsTag = tag.getList("Inputs", Tag.TAG_COMPOUND);
            for (Tag t : inputsTag) {
                AlchemyInput.CODEC.parse(NbtOps.INSTANCE, t)
                        .resultOrPartial(ArsAstra.LOGGER::error)
                        .ifPresent(loadedInputs::add);
            }
        }
        
        // Recompute on load
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

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- Getter 方法，供渲染器使用 ---

    /**
     * 获取当前的炼金上下文（包含路径和预测效果）
     */
    public StarChartContext getContext() {
        return context;
    }

    /**
     * 获取当前的流体等级 (0-3)
     */
    public int getFluidLevel() {
        return fluidLevel;
    }

    /**
     * 获取当前的流体类型 ID
     */
    public ResourceLocation getFluidType() {
        return fluidType;
    }

    /**
     * 客户端 Tick 逻辑，负责环境粒子表现
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CopperTunBlockEntity entity) {
        if (entity.fluidLevel > 0 && level.random.nextFloat() < 0.1f) {
            // 在液面产生一些基础气泡粒子
            double x = pos.getX() + 0.3 + level.random.nextDouble() * 0.4;
            double y = pos.getY() + 0.2 + (entity.fluidLevel * 0.25); // 根据液位调整高度
            double z = pos.getZ() + 0.3 + level.random.nextDouble() * 0.4;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE, x, y, z, 0, 0.02, 0);
        }
        
        // TODO: 未来可根据 context.currentRoute() 在空间中渲染代表要素路径的微弱粒子
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, 
            state -> state.setAndContinue(IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}