package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import java.util.List;
import java.util.Map;

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

    /**
     * 计算单个路径段与星图中效果星域的交互。
     */
    List<InteractionResult> computeInteractionsForSegment(StarChartPath segment, StarChart chart);

    /**
     * 根据几何交互结果计算最终的药水效果数据。
     *
     * @param interactions 交互结果列表
     * @return 效果字段到药水数据的映射
     */
    Map<EffectField, PotionData> calculateEffects(List<InteractionResult> interactions);
}
