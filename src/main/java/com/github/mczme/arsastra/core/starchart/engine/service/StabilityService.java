package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import java.util.List;

/**
 * 稳定性服务，评估当前炼金组合的稳定性。
 */
public interface StabilityService {
    /**
     * 计算当前的稳定度
     * @param inputs 当前的输入列表
     * @param decayCoefficient 容器的衰减系数 (1.0 = 标准衰减, 越小衰减越少)
     * @return 稳定度 (0.0 - 1.0)
     */
    float computeStability(List<AlchemyInput> inputs, float decayCoefficient);
}
