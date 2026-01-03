package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import org.joml.Vector2f;
import java.util.List;

/**
 * 路径生成服务，负责将投入的物品转化为星图上的航线。
 */
public interface RouteGenerationService {
    /**
     * 根据投入的物品列表及其操作参数计算航线。
     *
     * @param inputs     已投入的物品及参数列表
     * @param startPoint 起始坐标 (通常为容器的发射点)
     * @param chart      当前星图 (用于查询环境影响)
     * @return 生成的星图航线
     */
    StarChartRoute computeRoute(List<AlchemyInput> inputs, Vector2f startPoint, StarChart chart);

    /**
     * 计算分段航线数据，包含交互信息。用于推演和渲染。
     */
    List<SegmentData> computeSegmentedRoute(List<AlchemyInput> inputs, Vector2f startPoint, StarChart chart);
}
