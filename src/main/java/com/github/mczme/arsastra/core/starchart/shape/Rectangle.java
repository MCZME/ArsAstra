package com.github.mczme.arsastra.core.starchart.shape;

import org.joml.Vector2f;

/**
 * 代表一个轴对齐的矩形形状，由最小点和最大点定义。
 * @param min 矩形的最小坐标点（左下角）。
 * @param max 矩形的最大坐标点（右上角）。
 */
public record Rectangle(Vector2f min, Vector2f max) implements Shape {
    @Override
    public boolean contains(Vector2f point) {
        // 检查点的 x 和 y 坐标是否都在矩形的范围内。
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y;
    }
}
