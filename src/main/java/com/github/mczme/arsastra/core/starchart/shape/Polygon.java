package com.github.mczme.arsastra.core.starchart.shape;

import org.joml.Vector2f;

import java.util.List;

/**
 * 代表一个多边形形状，由一系列有序的顶点定义。
 * @param vertices 构成多边形的顶点列表，按顺时针或逆时针顺序排列。
 */
public record Polygon(List<Vector2f> vertices) implements Shape {
    @Override
    public boolean contains(Vector2f point) {
        // TODO: 实现点在多边形内部的算法 (例如：射线法或卷绕数法)。
        // 这是一个比较复杂的几何算法，超出了当前的任务范围，在此先留作待办。
        // 对于一个点 P(x, y) 和一个多边形，算法需要检查从 P 发出的一条射线与多边形边的交点数量。
        // 如果交点数为奇数，则点在多边形内部；如果为偶数，则在外部。
        // 还需要处理点在边上或顶点上的特殊情况。
        throw new UnsupportedOperationException("Polygon.contains() 方法尚未实现。");
    }
}
