package com.github.mczme.arsastra.core.starchart.path;

import org.joml.Vector2f;

/**
 * 代表一段在星图上生成的路径。
 * 它可以是线性的，也可以是曲线。
 */
public interface StarChartPath {

    /**
     * 获取路径的起始点坐标。
     * @return 起始点坐标
     */
    Vector2f getStartPoint();

    /**
     * 获取路径的终点坐标。
     * @return 终点坐标
     */
    Vector2f getEndPoint();

    /**
     * 获取路径的总长度。
     * @return 路径长度
     */
    float getLength();

    /**
     * 获取路径上从起点出发，行进了`distance`距离后的点的坐标。
     * 这是实现路径与星域交互、渲染等功能的核心。
     * @param distance 从起点出发的距离 (值的范围应在 0 到 getLength() 之间)
     * @return 路径上对应点的坐标
     */
    Vector2f getPointAtDistance(float distance);
}
