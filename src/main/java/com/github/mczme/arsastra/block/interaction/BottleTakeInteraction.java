package com.github.mczme.arsastra.block.interaction;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import com.github.mczme.arsastra.core.starchart.engine.StarChartContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 处理使用玻璃瓶提取炼金产物的交互。
 */
public class BottleTakeInteraction implements TunInteraction {
    @SuppressWarnings("null")
    @Override
    public Optional<InteractionResult> interact(AbstractTunBlockEntity entity, Player player, InteractionHand hand, ItemStack heldItem) {
        if (heldItem.getItem() == Items.GLASS_BOTTLE && entity.getFluidLevel() > 0) {
            ItemStack potionStack = new ItemStack(Items.POTION);
            StarChartContext context = entity.getContext();
            
            // 根据预测效果构建药水内容
            List<MobEffectInstance> effects = new ArrayList<>();
            for (Map.Entry<EffectField, PotionData> entry : context.predictedEffects().entrySet()) {
                 EffectField field = entry.getKey();
                 PotionData data = entry.getValue();
                 var effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(field.effect());
                 effectHolder.ifPresent(holder -> 
                     effects.add(new MobEffectInstance(holder, data.duration(), data.level()))
                 );
            }

            potionStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(PotionContents.getColor(effects)), effects));
            
            player.setItemInHand(hand, ItemUtils.createFilledResult(heldItem, player, potionStack));
            
            entity.setFluidLevel(entity.getFluidLevel() - 1);
            entity.setChanged();
            entity.sync();
            
            if (entity.getLevel() != null) {
                entity.getLevel().playSound(null, entity.getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            
            return Optional.of(InteractionResult.SUCCESS);
        }
        return Optional.empty();
    }
}
