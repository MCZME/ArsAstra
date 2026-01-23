package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 定义釜类方块的交互策略接口。
 */
public interface TunInteraction {
    /**
     * 执行交互逻辑。
     * 
     * @param entity 触发交互的方块实体
     * @param player 进行交互的玩家
     * @param hand 交互的手
     * @param heldItem 玩家手持的物品
     * @return 如果该策略匹配并处理了交互，返回 InteractionResult；否则返回 Optional.empty()
     */
    Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem);
}
