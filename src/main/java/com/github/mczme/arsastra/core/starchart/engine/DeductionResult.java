package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.EffectField;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * 推演系统的结果汇总。
 * 包含完整的路径、最终稳定度以及预测的产物信息。
 *
 * @param route          生成的完整几何路径，供客户端渲染
 * @param finalStability 最终稳定度 (0.0 - 1.0)
 * @param predictedEffects 预测的产物效果列表
 */
public record DeductionResult(
        StarChartRoute route,
        float finalStability,
        Map<EffectField, PotionData> predictedEffects
) {
    public static final StreamCodec<FriendlyByteBuf, DeductionResult> STREAM_CODEC = StreamCodec.composite(
            StarChartRoute.STREAM_CODEC, DeductionResult::route,
            ByteBufCodecs.FLOAT, DeductionResult::finalStability,
            ByteBufCodecs.map(HashMap::new, EffectField.STREAM_CODEC, PotionData.STREAM_CODEC), DeductionResult::predictedEffects,
            DeductionResult::new
    );
}
