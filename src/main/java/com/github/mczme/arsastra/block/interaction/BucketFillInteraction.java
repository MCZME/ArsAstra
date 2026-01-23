package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import net.minecraft.resources.ResourceLocation;
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
 * 处理使用水桶填充釜的交互。
 */
public class BucketFillInteraction implements TunInteraction {
    @SuppressWarnings("null")
    @Override
    public Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem) {
        if (heldItem.getItem() == Items.WATER_BUCKET && entity.getFluidLevel() < 3) {
            ResourceLocation waterLoc = ResourceLocation.withDefaultNamespace("water");
            
            if (!entity.isFluidValid(waterLoc)) {
                return Optional.of(InteractionResult.FAIL);
            }

            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, new ItemStack(Items.BUCKET)));
            entity.setFluidLevel(3);
            entity.setFluidType(waterLoc);
            entity.resetContext();
            entity.setChanged();
            entity.sync();
            
            if (entity.getLevel() != null) {
                entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            
            return Optional.of(InteractionResult.SUCCESS);
        }
        return Optional.empty();
    }
}
