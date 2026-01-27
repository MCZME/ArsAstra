package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GravitySlingshotEnvironmentType implements EnvironmentType {

    @Override
    public List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        Vector2f center = environment.shape().getCenter();
        float diameter = environment.shape().getCharacteristicSize();

        // 1. 计算从当前入口点到中心的矢量
        Vector2f toCenter = new Vector2f(center).sub(currentStart);
        float distToCenter = toCenter.length();

        // 2. 确定发射方向
        // 如果已经非常接近中心，则使用原路径方向；否则使用 入口->中心 的方向
        Vector2f launchDirection;
        if (distToCenter < 0.001f) {
            if (originalPath instanceof LinearStarChartPath linear) {
                launchDirection = new Vector2f(linear.getEndPoint()).sub(linear.getStartPoint()).normalize();
            } else {
                launchDirection = new Vector2f(1, 0); // Fallback
            }
        } else {
            launchDirection = new Vector2f(toCenter).normalize();
        }

        // 3. 计算发射长度
        // 保底长度为直径，如果原剩余动能(路径长度)更大，则保留动能
        float launchLength = Math.max(diameter, originalPath.getLength());

        List<StarChartPath> results = new ArrayList<>();

        // 段1：被吸入中心 (如果距离不为0)
        if (distToCenter >= 0.001f) {
            results.add(new LinearStarChartPath(new Vector2f(0, 0), toCenter).offset(currentStart));
        }

        // 段2：从中心发射
        Vector2f launchVector = new Vector2f(launchDirection).mul(launchLength);
        results.add(new LinearStarChartPath(new Vector2f(0, 0), launchVector).offset(center));

        return results;
    }
}
