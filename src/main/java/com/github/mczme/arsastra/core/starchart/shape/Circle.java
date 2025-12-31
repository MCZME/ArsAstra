package com.github.mczme.arsastra.core.starchart.shape;

import org.joml.Vector2f;

/**
 * 代表一个圆形形状，由圆心点和半径定义。
 * @param center 圆心坐标。
 * @param radius 半径长度。
 */
public record Circle(Vector2f center, float radius) implements Shape {
    @Override
    public boolean contains(Vector2f point) {
        // 使用距离的平方来避免昂贵的开方运算，判断点是否在圆内。
        return point.distanceSquared(center) <= radius * radius;
    }
}
