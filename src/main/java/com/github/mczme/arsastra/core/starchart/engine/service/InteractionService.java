package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import java.util.List;

/**
 * 交互处理服务，计算航线与星图星域的几何交互。
 */
public interface InteractionService {
    /**
     * 计算航线与星图中所有效果星域的交互。
     *
     * @param route 炼金航线
     * @param chart 星图
     * @return 交互结果列表
     */
    List<InteractionResult> computeInteractions(StarChartRoute route, StarChart chart);
}
