package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.registry.AABlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class CopperTunBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final software.bernie.geckolib.animation.RawAnimation IDLE_ANIM = software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.copper_tun.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CopperTunBlockEntity(BlockPos pos, BlockState state) {
        super(AABlockEntities.COPPER_TUN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CopperTunBlockEntity entity) {
        // 1. Heat Check (MVP: check every tick)
        if (checkHeat(level, pos.below())) {
            handleItemInput(level, pos, entity);
        }
    }

    private static boolean checkHeat(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    private static void handleItemInput(Level level, BlockPos pos, CopperTunBlockEntity entity) {
        // Capture area: Inside the tun
        AABB captureArea = new AABB(pos.getX() + 0.2, pos.getY() + 0.2, pos.getZ() + 0.2,
                                    pos.getX() + 0.8, pos.getY() + 1.0, pos.getZ() + 0.8);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, captureArea);

        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;

            ItemStack stack = itemEntity.getItem();
            int count = stack.getCount();

            // Log for debug
            ArsAstra.LOGGER.info("Copper Tun absorbed {} x {}", count, stack.getHoverName().getString());

            // Placeholder for Alchemy Logic: Process stack one by one
            for (int i = 0; i < count; i++) {
                // TODO: Add alchemy logic here
            }

            // Visual/Audio Feedback
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.2f, 1.0f);
            
            // Consume item
            itemEntity.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "controller", 0, 
            state -> state.setAndContinue(IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}