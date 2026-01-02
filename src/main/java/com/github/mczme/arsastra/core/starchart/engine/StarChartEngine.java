package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.StarChart;
import org.joml.Vector2f;

/**
 * 星图引擎，逻辑层的核心入口。
 */
public interface StarChartEngine {
    /**
     * 进行一次完整的炼金/推演计算。
     *
     * @param chart 当前使用的星图
     * @param context 包含输入状态的上下文 (主要使用其 thrownItems)
     * @param startPoint 起始坐标
     * @return 包含计算结果的全新上下文
     */
    StarChartContext compute(StarChart chart, StarChartContext context, Vector2f startPoint);
}
