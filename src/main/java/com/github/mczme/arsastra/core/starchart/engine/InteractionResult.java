package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.EffectField;

/**
 * 记录路径与效果星域的一次交互结果。
 *
 * @param field 涉及的效果星域
 * @param arcLength 在星域内部穿越的路径长度（场内弧长）
 * @param periapsisDistance 轨道近心点距离（最小距离）
 */
public record InteractionResult(
        EffectField field,
        float arcLength,
        float periapsisDistance
) {
}
