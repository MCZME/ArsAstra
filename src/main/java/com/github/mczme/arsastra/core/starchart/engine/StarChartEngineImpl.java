package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.service.*;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StarChartEngineImpl implements StarChartEngine {

    private final RouteGenerationService routeService = new RouteGenerationServiceImpl();
    private final InteractionService interactionService = new InteractionServiceImpl();
    private final StabilityService stabilityService = new StabilityServiceImpl();

    private static final float DURATION_MULTIPLIER = 20.0f; // 每 1.0 弧长 = 1秒 (20 ticks)

    @Override
    public StarChartContext compute(StarChart chart, StarChartContext context, Vector2f startPoint) {
        // 1. 生成路径 (传入的是完整的 context.inputs() 和 当前星图)
        StarChartRoute route = routeService.computeRoute(context.inputs(), startPoint, chart);

        // 2. 计算交互
        List<InteractionResult> interactions = interactionService.computeInteractions(route, chart);

        // 3. 计算稳定性
        float stability = stabilityService.computeStability(context.inputs());

        // 4. 计算最终效果 (PotionData)
        Map<EffectField, PotionData> predictedEffects = calculateEffects(interactions);

        // 返回全新的上下文
        return new StarChartContext(
                context.inputs(),
                route,
                interactions,
                stability,
                predictedEffects
        );
    }

    private Map<EffectField, PotionData> calculateEffects(List<InteractionResult> interactions) {
        Map<EffectField, PotionData> results = new HashMap<>();

        for (InteractionResult result : interactions) {
            EffectField field = result.field();
            
            // 计算等级：基于近心点距离。距离越近，等级越高，最高为 maxLevel
            // 如果近心点刚好在边缘，等级为 0 (Level 1)；如果在中心，等级为 maxLevel
            float radius = field.getRadius();
            float distRatio = Math.max(0, 1.0f - (result.periapsisDistance() / radius));
            int calculatedLevel = Math.round(field.maxLevel() * distRatio);
            // 确保至少 Level 1 (amplifier 0)
            calculatedLevel = Math.max(0, calculatedLevel);

            // 计算时长：基于场内弧长累加
            int calculatedDuration = Math.round(result.arcLength() * DURATION_MULTIPLIER);

            PotionData newData = new PotionData(calculatedLevel, calculatedDuration);

            // 如果该效果已存在（多次穿越），则进行合并
            results.merge(field, newData, PotionData::merge);
        }

        return results;
    }
}
