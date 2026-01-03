package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import java.util.List;

/**
 * 稳定性服务，评估当前炼金组合的稳定性。
 */
public interface StabilityService {
    /**
     * 计算当前物品组合的稳定性。
     *
     * @param inputs 投入的物品及参数列表
     * @return 稳定性 (0.0 - 1.0)
     */
    float computeStability(List<AlchemyInput> inputs);
}
