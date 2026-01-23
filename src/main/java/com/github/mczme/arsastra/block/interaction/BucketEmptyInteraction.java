package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * 处理使用空桶提取釜内流体的交互。
 */
public class BucketEmptyInteraction implements TunInteraction {
    @SuppressWarnings("null")
    @Override
    public Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem) {
        if (heldItem.getItem() == Items.BUCKET && entity.getFluidLevel() > 0) {
            // 目前仅支持水，未来可扩展根据 fluidType 返回对应的桶
            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, new ItemStack(Items.WATER_BUCKET)));
            
            entity.setFluidLevel(0);
            entity.resetContext();
            entity.setChanged();
            entity.sync();
            
            if (entity.getLevel() != null) {
                entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            
            return Optional.of(InteractionResult.SUCCESS);
        }
        return Optional.empty();
    }
}
