package com.github.mczme.arsastra.core.starchart.shape;

import org.joml.Vector2f;

import java.util.List;

/**
 * 代表一个“外部”多边形形状，用于定义边界。
 * 当一个点在由顶点定义的多边形“外部”时，contains 方法才返回 true。
 * @param vertices 构成内部边界的顶点列表。
 */
public record ExteriorPolygon(List<Vector2f> vertices) implements Shape {
    @Override
    public boolean contains(Vector2f point) {
        // TODO: 实现点在多边形内部的算法，然后返回相反的结果。
        // 最终逻辑应为: return !isPointInPolygon(this.vertices, point);
        throw new UnsupportedOperationException("ExteriorPolygon.contains() 方法尚未实现。");
    }
}
