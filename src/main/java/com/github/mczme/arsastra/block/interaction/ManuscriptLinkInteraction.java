package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.item.ManuscriptItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 处理手稿对釜的绑定交互，启动酿造指引模式。
 */
public class ManuscriptLinkInteraction implements TunInteraction {
    @SuppressWarnings("null")
    @Override
    public Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem) {
        if (heldItem.getItem() instanceof ManuscriptItem) {
            // 釜必须有液体才能进行炼金指引
            if (entity.getFluidLevel() <= 0) {
                return Optional.of(InteractionResult.PASS);
            }

            // 提取手稿数据
            Optional<ClientManuscript> manuscriptOpt = ManuscriptItem.getManuscript(heldItem);
            if (manuscriptOpt.isPresent()) {
                if (!entity.getLevel().isClientSide) {
                    // 启动指引
                    entity.startGuidedBrewing(manuscriptOpt.get().inputs());
                    
                    // 消耗手稿
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    
                    entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 0.8f);
                    entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
                }
                return Optional.of(InteractionResult.sidedSuccess(entity.getLevel().isClientSide));
            }
        }
        return Optional.empty();
    }
}
