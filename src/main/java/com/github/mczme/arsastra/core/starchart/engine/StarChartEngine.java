package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
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

    /**
     * 进行一次完整的炼金/推演计算。
     * 自动从第一个输入物品中解析起始坐标 (Launch Point)，若无输入则默认为 (0,0)。
     *
     * @param chart 当前使用的星图
     * @param context 包含输入状态的上下文
     * @return 包含计算结果的全新上下文
     */
    default StarChartContext compute(StarChart chart, StarChartContext context) {
        Vector2f startPoint = new Vector2f(0, 0);
        if (!context.inputs().isEmpty()) {
            AlchemyInput firstInput = context.inputs().get(0);
            startPoint = ElementProfileManager.getInstance()
                    .getElementProfile(firstInput.stack().getItem())
                    .map(p -> new Vector2f(p.launchPoint()))
                    .orElse(new Vector2f(0, 0));
        }
        return compute(chart, context, startPoint);
    }
}
