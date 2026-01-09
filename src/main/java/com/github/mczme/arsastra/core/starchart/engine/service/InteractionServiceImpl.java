package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractionServiceImpl implements InteractionService {

    private static final float SAMPLE_STEP = 1.0f; // 采样步长
    private static final float DURATION_MULTIPLIER = 20.0f; // 每 1.0 弧长 = 1秒 (20 ticks)

    @Override
    public List<InteractionResult> computeInteractions(StarChartRoute route, StarChart chart) {
        // 为了保持语义一致（即同一种星域的交互结果合并），
        // 这里的实现仍然会对整条航线产生的同名星域交互进行累加。
        Map<EffectField, Float> arcLengths = new HashMap<>();
        Map<EffectField, Float> minDistances = new HashMap<>();

        for (StarChartPath segment : route.segments()) {
            List<InteractionResult> segmentResults = computeInteractionsForSegment(segment, chart);
            for (InteractionResult res : segmentResults) {
                arcLengths.merge(res.field(), res.arcLength(), Float::sum);
                minDistances.merge(res.field(), res.periapsisDistance(), Math::min);
            }
        }

        List<InteractionResult> results = new ArrayList<>();
        arcLengths.forEach((field, length) -> {
            results.add(new InteractionResult(field, length, minDistances.get(field)));
        });

        return results;
    }

    @Override
    public List<InteractionResult> computeInteractionsForSegment(StarChartPath segment, StarChart chart) {
        List<InteractionResult> results = new ArrayList<>();
        float length = segment.getLength();
        if (length <= 0) return results;

        for (EffectField field : chart.fields()) {
            float totalArcLength = 0;
            float minDistance = Float.MAX_VALUE;
            float radius = field.getRadius();
            Vector2f center = field.center();

            boolean interacted = false;

            // 步进采样计算场内弧长和最小距离
            for (float d = 0; d <= length; d += SAMPLE_STEP) {
                Vector2f point = segment.getPointAtDistance(d);
                float dist = point.distance(center);
                
                if (dist < minDistance) {
                    minDistance = dist;
                }

                if (dist <= radius) {
                    totalArcLength += Math.min(SAMPLE_STEP, length - d); 
                    interacted = true;
                }
            }
            
            // 检查终点（防止采样步长导致的遗漏）
            Vector2f endPoint = segment.getEndPoint();
            float endDist = endPoint.distance(center);
            if (endDist < minDistance) minDistance = endDist;

            if (interacted) {
                results.add(new InteractionResult(field, totalArcLength, minDistance));
            }
        }
        return results;
    }

    @Override
    public Map<EffectField, PotionData> calculateEffects(List<InteractionResult> interactions) {
        Map<EffectField, PotionData> results = new HashMap<>();

        for (InteractionResult result : interactions) {
            EffectField field = result.field();
            
            // 计算等级：阶梯式计算
            // 规则：最近距离 <= n * 20 时，等级 = maxLevel - n + 1
            float dist = result.periapsisDistance();
            int n = (int) Math.ceil(dist / 20.0f);
            if (n < 1) n = 1; // 即使在圆心，n 也是 1
            
            int displayLevel = field.maxLevel() - n + 1;
            
            // 药水等级 (Amplifier) = 显示等级 - 1
            // 确保等级不会低于 0 (Level 1)
            int amplifier = Math.max(0, displayLevel - 1);

            // 计算时长：基于场内弧长累加
            int calculatedDuration = Math.round(result.arcLength() * DURATION_MULTIPLIER);

            if (calculatedDuration > 0) {
                 results.merge(field, new PotionData(amplifier, calculatedDuration), PotionData::merge);
            }
        }
        return results;
    }
}
