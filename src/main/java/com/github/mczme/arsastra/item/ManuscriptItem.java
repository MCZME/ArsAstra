package com.github.mczme.arsastra.item;

import com.github.mczme.arsastra.client.gui.screen.ManuscriptViewScreen;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import com.github.mczme.arsastra.registry.AAComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

public class ManuscriptItem extends Item {
    public ManuscriptItem(Properties properties) {
        super(properties);
    }

    /**
     * 从物品堆中提取手稿数据。
     */
    public static Optional<ClientManuscript> getManuscript(ItemStack stack) {
        return Optional.ofNullable(stack.get(AAComponents.MANUSCRIPT_DATA));
    }

    /**
     * 将手稿数据写入物品堆。
     */
    public static void setManuscript(ItemStack stack, ClientManuscript manuscript) {
        stack.set(AAComponents.MANUSCRIPT_DATA, manuscript);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        getManuscript(stack).ifPresent(manuscript -> {
            if (!manuscript.name().isEmpty()) {
                tooltipComponents.add(Component.literal(manuscript.name()).withStyle(ChatFormatting.GOLD));
            }
        });
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            getManuscript(stack).ifPresent(manuscript -> {
                openViewScreen(manuscript);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void openViewScreen(ClientManuscript manuscript) {
        // 我们需要一个轻量级的 Screen 来承载 Overlay
        Minecraft.getInstance().setScreen(new ManuscriptViewScreen(manuscript));
    }
}
