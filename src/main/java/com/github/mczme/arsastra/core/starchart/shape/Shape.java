package com.github.mczme.arsastra.core.starchart.shape;

import org.joml.Vector2f;

/**
 * 这是一个通用的接口，代表了任意二维形状。
 * 其主要目的是检查点是否在形状内部（碰撞检测/包含性检查）。
 */
public interface Shape {
    /**
     * 检查一个给定点是否在该形状的内部。
     * @param point 要检查的点。
     * @return 如果点在形状内部，则返回 true；否则返回 false。
     */
    boolean contains(Vector2f point);
}
