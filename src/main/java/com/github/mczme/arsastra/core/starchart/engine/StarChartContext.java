package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.EffectField;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 星图上下文，封装了单次炼金过程的所有状态。
 *
 * @param inputs 已投入的物品及操作参数列表
 * @param currentRoute 当前生成的星图航线
 * @param interactions 当前航线与星域的交互结果
 * @param stability 当前的稳定性 (0.0 - 1.0)
 * @param predictedEffects 最终预测的效果、等级、时长
 */
public record StarChartContext(
        List<AlchemyInput> inputs,
        StarChartRoute currentRoute,
        List<InteractionResult> interactions,
        float stability,
        Map<EffectField, PotionData> predictedEffects
) {
    public List<ItemStack> getThrownItems() {
        return inputs.stream().map(AlchemyInput::stack).collect(Collectors.toList());
    }
}
