package com.github.mczme.arsastra.core.starchart.engine.service;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public class StabilityServiceImpl implements StabilityService {
    @Override
    public float computeStability(List<ItemStack> items) {
        if (items.isEmpty()) return 1.0f;
        
        // 简单的演示逻辑：每增加一个物品，稳定性降低 5%，最低 20%
        float stability = 1.0f - (items.size() * 0.05f);
        return Math.max(0.2f, stability);
    }
}
