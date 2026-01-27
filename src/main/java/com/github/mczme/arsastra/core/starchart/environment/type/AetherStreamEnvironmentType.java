package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class AetherStreamEnvironmentType implements EnvironmentType {

    @Override
    public List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        // 1. 获取流速矢量
        float flowX = 0.0f;
        float flowY = 0.0f;
        try {
            if (environment.data().containsKey("flow_x")) flowX = Float.parseFloat(environment.data().get("flow_x"));
            if (environment.data().containsKey("flow_y")) flowY = Float.parseFloat(environment.data().get("flow_y"));
        } catch (NumberFormatException ignored) {}

        Vector2f flow = new Vector2f(flowX, flowY);

        // 如果没有流速，直接返回
        if (flow.lengthSquared() < 0.0001f) {
            return Collections.singletonList(originalPath.offset(currentStart));
        }

        // 2. 应用矢量漂移
        // 目前仅支持线性路径的矢量合成
        if (originalPath instanceof LinearStarChartPath linear) {
            // 结果矢量 = 原始矢量 + 流速矢量 (假设流速作用于整段路径)
            // 也可以理解为：实际位移 = 船速 + 水流速
            Vector2f linearVector = new Vector2f(linear.getEndPoint()).sub(linear.getStartPoint());
            Vector2f newVector = linearVector.add(flow);
            
            return Collections.singletonList(new LinearStarChartPath(new Vector2f(0, 0), newVector).offset(currentStart));
        }

        // 对于非线性路径（如弧线），简单的矢量加法可能不适用，暂不处理漂移，仅平移
        return Collections.singletonList(originalPath.offset(currentStart));
    }
}
