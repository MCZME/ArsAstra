package com.github.mczme.arsastra.core.element;

import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/**
 * 代表一个“特殊要素”的抽象基类。
 * 它不提供直接位移，而是作为“路径塑形引擎”。
 */
public abstract class SpecialElement implements Element {
    @Override
    public final StarChartPath getPath(float specialStrength, Map<ResourceLocation, Float> basicElements) {
        // 将调用委托给一个具体的、由子类实现的塑形方法
        return this.shapePath(specialStrength, basicElements);
    }

    /**
     * 由每个具体的特殊要素子类（如 MindElement）实现的独特塑形算法。
     */
    protected abstract StarChartPath shapePath(float specialStrength, Map<ResourceLocation, Float> basicElements);
}
