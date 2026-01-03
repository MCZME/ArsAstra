package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import org.joml.Vector2f;

import java.util.List;

/**
 * 推演服务，提供炼金过程的模拟功能。
 * 它是 GUI 预览和实际炼金执行的核心支撑。
 */
public interface DeductionService {
    /**
     * 模拟整个炼金过程并返回详细结果。
     *
     * @param chart      当前星图
     * @param inputs     材料投入序列
     * @param startPoint 发射起点
     * @return 包含路径动画数据、稳定度、交互结果的推演报告
     */
    DeductionResult deduce(StarChart chart, List<AlchemyInput> inputs, Vector2f startPoint);
}
