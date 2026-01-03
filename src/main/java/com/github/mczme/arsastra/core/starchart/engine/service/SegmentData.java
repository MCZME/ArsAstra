package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import java.util.List;

/**
 * 分段路径数据，用于前端进行“航线生长”动画的展示。
 * 对应于每一次投入物品所产生的那一段或多段物理路径。
 *
 * @param path         该段航线的几何形状
 * @param interactions 该段航线产生的交互事件
 * @param rotation     该段航线被施加的旋转偏移 (用于可视化搅拌动作)
 */
public record SegmentData(
        StarChartPath path,
        List<InteractionResult> interactions,
        float rotation
) {
}
