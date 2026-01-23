package com.github.mczme.arsastra.item;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 搅拌棒物品类。
 */
public class StirringStickItem extends Item {
    public StirringStickItem(Properties properties) {
        super(properties);
    }

    /**
     * 创建物品属性修改器（攻击伤害和速度）。
     */
    public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, (double)((float)attackDamage + tier.getAttackDamageBonus()), AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, (double)attackSpeed, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
