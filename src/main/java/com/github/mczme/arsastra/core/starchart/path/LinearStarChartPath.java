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

    @Override
    public StarChartPath offset(Vector2f offset) {
        return new LinearStarChartPath(
                new Vector2f(this.startPoint).add(offset),
                new Vector2f(this.endPoint).add(offset)
        );
    }

    @Override
    public StarChartPath rotate(float angle) {
        Vector2f relativeEnd = new Vector2f(this.endPoint).sub(this.startPoint);
        
        // 应用旋转
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float newX = relativeEnd.x * cos - relativeEnd.y * sin;
        float newY = relativeEnd.x * sin + relativeEnd.y * cos;
        
        relativeEnd.set(newX, newY);
        
        return new LinearStarChartPath(
                new Vector2f(this.startPoint),
                new Vector2f(this.startPoint).add(relativeEnd)
        );
    }

    @Override
    public StarChartPath[] split(float distance) {
        float length = getLength();
        if (distance <= 0.001f) {
            // 切割点在起点，前半段为空(或极小)，这里简单处理：全部归为后半段
            // 但为了接口约定，我们还是返回一个零长度路径作为前半段
            return new StarChartPath[]{
                    new LinearStarChartPath(new Vector2f(startPoint), new Vector2f(startPoint)),
                    this
            };
        }
        if (distance >= length - 0.001f) {
            return new StarChartPath[]{
                    this,
                    new LinearStarChartPath(new Vector2f(endPoint), new Vector2f(endPoint))
            };
        }

        Vector2f splitPoint = getPointAtDistance(distance);
        return new StarChartPath[]{
                new LinearStarChartPath(new Vector2f(startPoint), new Vector2f(splitPoint)),
                new LinearStarChartPath(new Vector2f(splitPoint), new Vector2f(endPoint))
        };
    }
}
