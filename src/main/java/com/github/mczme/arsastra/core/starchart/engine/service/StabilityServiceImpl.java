package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import java.util.List;

public class StabilityServiceImpl implements StabilityService {
    @Override
    public float computeStability(List<AlchemyInput> inputs, float decayCoefficient) {
        if (inputs.isEmpty()) return 1.0f;
        
        float quantityPenalty = 0.0f;
        float rotationPenalty = 0.0f;
        float voidPenalty = 0.0f;

        for (AlchemyInput input : inputs) {
            // 1. 数量惩罚: 每个物品 5%
            quantityPenalty += 0.05f;

            // 2. 旋转惩罚: 每 1 度偏移 1%
            rotationPenalty += Math.abs(input.rotation()) * 0.01f;

            // 3. 杂质检查: 无要素物品每个 30%
            if (ElementProfileManager.getInstance().getElementProfile(input.stack().getItem()).isEmpty()) {
                voidPenalty += 0.3f;
            }
        }

        // 应用系数 (注意：杂质惩罚不受容器系数减免)
        float totalDecay = (quantityPenalty + rotationPenalty) * decayCoefficient + voidPenalty;
        
        return Math.max(0.0f, 1.0f - totalDecay);
    }
}
