package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 集中管理所有釜类方块的交互策略。
 */
public class TunInteractions {
    private static final List<TunInteraction> INTERACTIONS = new ArrayList<>();

    // 静态初始化，按优先级添加交互逻辑
    static {
        register(new BucketFillInteraction());
        register(new BucketEmptyInteraction());
        register(new BottleTakeInteraction());
        register(new ManuscriptLinkInteraction());
        register(new StirringInteraction());
    }

    /**
     * 遍历并执行匹配的交互策略。
     */
    public static InteractionResult handleInteraction(AbstractTunBlockEntity entity, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        
        for (TunInteraction interaction : INTERACTIONS) {
            Optional<InteractionResult> result = interaction.interact(entity, player, hand, heldItem);
            if (result.isPresent()) {
                return result.get();
            }
        }
        
        return InteractionResult.PASS;
    }

    /**
     * 注册新的交互策略（外部扩展用）。
     */
    public static void register(TunInteraction interaction) {
        INTERACTIONS.add(interaction);
    }
}
