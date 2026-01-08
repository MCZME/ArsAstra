package com.github.mczme.arsastra.core.element;

import com.mojang.serialization.Codec;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.registry.AARegistries;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import java.util.Map;

/**
 * 代表一个“要素”的通用接口。
 * 要素是构成物品炼金属性的基本单位。
 */
public interface Element {

    Codec<Element> CODEC = ResourceLocation.CODEC.xmap(
            AARegistries.ELEMENT_REGISTRY::get,
            element -> AARegistries.ELEMENT_REGISTRY.getKey(element)
    );

    Vector2f getVector();

    /**
     * 【基础要素】使用此方法生成路径。
     * 基于要素自身的矢量和传入的强度，生成一个简单的线性路径。
     * @param strength 要素强度
     * @return 代表路径的对象
     */
    default StarChartPath getPath(float strength) {
        // 特殊要素不应调用此方法
        throw new UnsupportedOperationException("This element requires a basic element vector to shape a path.");
    }

    /**
     * 【特殊要素】使用此方法生成路径。
     * 基于自身的强度和来自其他基础要素的合成矢量，生成一个复杂的塑形路径。
     * @param specialStrength 特殊要素自身的强度
     * @param basicElements 同一物品中所有基础要素的ID及其强度
     * @return 代表路径的对象
     */
    default StarChartPath getPath(float specialStrength, Map<ResourceLocation, Float> basicElements) {
        // 基础要素不应调用此方法
        throw new UnsupportedOperationException("This element does not shape a path with a basic element vector.");
    }

    default String getDescriptionId() {

        return Util.makeDescriptionId("element", AARegistries.ELEMENT_REGISTRY.getKey(this));

    }

    /**
    * 获取要素的图标纹理位置。
    * 路径为 assets/<namespace>/textures/element/<path>.png
    */
    @SuppressWarnings("null")
    default ResourceLocation getIcon() {
        ResourceLocation key = AARegistries.ELEMENT_REGISTRY.getKey(this);
        return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/element/" + key.getPath() + ".png");
    }

} 