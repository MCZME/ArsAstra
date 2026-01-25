package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.registry.AAItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 处理搅拌棒的插入、拔出及搅拌操作。
 */
public class StirringInteraction implements TunInteraction {
    @SuppressWarnings("null")
    @Override
    public Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem) {
        ItemStack stickInTun = entity.getStirringStick();
        boolean hasStick = !stickInTun.isEmpty();

        // 1. 插棒: 手持搅拌棒且釜中没有棒
        if (!hasStick && heldItem.is(AAItems.STIRRING_STICK.get())) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            entity.setStirringStick(toInsert);
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            
            entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            entity.setChanged();
            entity.sync();
            return Optional.of(InteractionResult.SUCCESS);
        }

        // 2. 搅拌: 釜中有棒
        // 只有在空手、有液体且有投入物品时才能搅拌
        if (hasStick && heldItem.isEmpty() && entity.getFluidLevel() > 0 && !entity.getContext().inputs().isEmpty()) {
            // 正在动画中，拒绝交互
            if (entity.isStirring()) {
                return Optional.of(InteractionResult.FAIL);
            }

            // Shift = 逆时针，普通 = 顺时针
            boolean clockwise = !player.isShiftKeyDown();
                    // 执行搅拌
                    performStir(entity, clockwise);
                    entity.checkStir(clockwise);
                    return Optional.of(InteractionResult.SUCCESS);
        }

        return Optional.empty();
    }

    @SuppressWarnings("null")
    private void performStir(AbstractTunBlockEntity entity, boolean clockwise) {
        // 1. 设置动画状态
        entity.setStirring(true);
        entity.setStirProgress(0.0f);
        entity.setStirringClockwise(!clockwise);
        
        // 2. 修改逻辑数据 (旋转最后一个物品)
        List<AlchemyInput> inputs = entity.getContext().inputs();
        if (!inputs.isEmpty()) {
            int lastIndex = inputs.size() - 1;
            AlchemyInput lastInput = inputs.get(lastIndex);
            
            // 旋转 5 度
            float delta = clockwise ? 5.0f : -5.0f;
            float newRotation = lastInput.rotation() + delta;
            
            // 创建新的 Input 列表
            List<AlchemyInput> newInputs = new ArrayList<>(inputs);
            newInputs.set(lastIndex, lastInput.withRotation(newRotation));
            
            // 重算星图
            entity.computeContext(newInputs);
        }
        
        // 3. 播放音效
        // 使用自定义搅拌音效 (基于 grindstone 模拟机械/摩擦感)
        entity.getLevel().playSound(null, entity.getBlockPos(), com.github.mczme.arsastra.registry.AASounds.STIRRING.get(), SoundSource.BLOCKS, 0.6f, 1.2f + entity.getLevel().getRandom().nextFloat() * 0.4f);
        
        entity.setChanged();
        entity.sync();
    }
}
