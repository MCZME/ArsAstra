package com.github.mczme.arsastra.core.starchart.path;

import org.joml.Vector2f;

public class LinearStarChartPath implements StarChartPath {
    private final Vector2f startPoint;
    private final Vector2f endPoint;

    public LinearStarChartPath(Vector2f startPoint, Vector2f endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    @Override
    public Vector2f getStartPoint() {
        return new Vector2f(this.startPoint);
    }

    @Override
    public Vector2f getEndPoint() {
        return new Vector2f(this.endPoint);
    }

    @Override
    public float getLength() {
        return this.startPoint.distance(this.endPoint);
    }

    @Override
    public Vector2f getPointAtDistance(float distance) {
        if (distance <= 0) {
            return getStartPoint();
        }
        float length = getLength();
        if (distance >= length) {
            return getEndPoint();
        }
        // 根据距离计算比例
        float t = distance / length;
        return new Vector2f(this.startPoint).lerp(this.endPoint, t);
    }
}
