package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class InteractionServiceImpl implements InteractionService {

    private static final float SAMPLE_STEP = 1.0f; // 采样步长

    @Override
    public List<InteractionResult> computeInteractions(StarChartRoute route, StarChart chart) {
        List<InteractionResult> results = new ArrayList<>();

        for (EffectField field : chart.fields()) {
            float totalArcLength = 0;
            float minDistance = Float.MAX_VALUE;
            float radius = field.getRadius();
            Vector2f center = field.center();

            boolean interacted = false;

            for (StarChartPath segment : route.segments()) {
                float length = segment.getLength();
                if (length <= 0) continue;

                // 步进采样计算场内弧长和最小距离
                for (float d = 0; d <= length; d += SAMPLE_STEP) {
                    Vector2f point = segment.getPointAtDistance(d);
                    float dist = point.distance(center);
                    
                    if (dist < minDistance) {
                        minDistance = dist;
                    }

                    if (dist <= radius) {
                        totalArcLength += SAMPLE_STEP; // 这里简化处理，采样步长即为增加的弧长
                        interacted = true;
                    }
                }
                
                // 检查终点（防止采样步长导致的遗漏）
                Vector2f endPoint = segment.getEndPoint();
                float endDist = endPoint.distance(center);
                if (endDist < minDistance) minDistance = endDist;
                // 注意：这里为了不重复计算，不在这里增加 arcLength
            }

            if (interacted) {
                results.add(new InteractionResult(field, totalArcLength, minDistance));
            }
        }

        return results;
    }
}
