package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import java.util.List;

public class StabilityServiceImpl implements StabilityService {
    @Override
    public float computeStability(List<AlchemyInput> inputs) {
        if (inputs.isEmpty()) return 1.0f;
        
        // 简单的演示逻辑：每增加一个物品，稳定性降低 5%，最低 20%
        float stability = 1.0f - (inputs.size() * 0.05f);
        return Math.max(0.2f, stability);
    }
}
