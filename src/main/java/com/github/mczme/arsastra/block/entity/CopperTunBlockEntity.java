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
    private static final RawAnimation BOIL_MILD = RawAnimation.begin().thenLoop("animation.copper_tun.boil_mild");
    private static final RawAnimation BOIL_INTENSE = RawAnimation.begin().thenLoop("animation.copper_tun.boil_intense");

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
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.fluidLevel > 0) {
                float stability = getContext().stability();
                // 临界状态: 剧烈晃动
                if (stability < 0.2f) {
                    return state.setAndContinue(BOIL_INTENSE);
                } 
                // 低稳定状态: 轻微晃动
                else if (stability < 0.4f) {
                    return state.setAndContinue(BOIL_MILD);
                }
            }
            // 正常或空载: 保持静止
            return state.setAndContinue(IDLE_ANIM);
        }));
    }
}
