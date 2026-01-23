package com.github.mczme.arsastra.block.entity;

import com.github.mczme.arsastra.registry.AABlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class CopperTunBlockEntity extends AbstractTunBlockEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.copper_tun.idle");

    public CopperTunBlockEntity(BlockPos pos, BlockState state) {
        super(AABlockEntities.COPPER_TUN.get(), pos, state);
    }

    @Override
    public int getMaxInputCount() {
        return 8; // Tier 1 限制
    }

    @Override
    public boolean isFluidValid(ResourceLocation fluid) {
        return fluid.getPath().equals("water"); // Tier 1 仅支持水
    }

    @Override
    public float getStabilityCoefficient() {
        return 1.0f; // Tier 1: 标准衰减
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, 
            state -> state.setAndContinue(IDLE_ANIM)));
    }
}
