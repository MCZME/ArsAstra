package com.github.mczme.arsastra.core.starchart.engine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 代表炼金过程中的一次单一输入操作及其相关参数。
 * 包含了物品本身以及玩家施加的操作（如搅拌）。
 */
public record AlchemyInput(
        ItemStack stack,
        float rotation
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyInput> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            AlchemyInput::stack,
            ByteBufCodecs.FLOAT,
            AlchemyInput::rotation,
            AlchemyInput::new
    );

    public static AlchemyInput of(ItemStack stack) {
        return new AlchemyInput(stack, 0.0f);
    }

    public AlchemyInput withRotation(float rotation) {
        return new AlchemyInput(this.stack, rotation);
    }
}
