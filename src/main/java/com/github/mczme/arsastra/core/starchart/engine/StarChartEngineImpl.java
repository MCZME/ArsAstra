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

    @Override
    public StarChartContext compute(StarChart chart, StarChartContext context, Vector2f startPoint, float decayCoefficient) {
        // 1. 生成路径 (传入的是完整的 context.inputs() 和 当前星图)
        StarChartRoute route = routeService.computeRoute(context.inputs(), startPoint, chart);

        // 2. 计算交互
        List<InteractionResult> interactions = interactionService.computeInteractions(route, chart);

        // 3. 计算稳定性
        float stability = stabilityService.computeStability(context.inputs(), decayCoefficient);

        // 4. 计算最终效果 (PotionData)
        Map<EffectField, PotionData> predictedEffects = interactionService.calculateEffects(interactions);

        // 5. 应用稳定性修正
        // Duration = Base * Stability
        // Level = Stability < 0.4 ? max(0, Level - 1) : Level
        Map<EffectField, PotionData> finalEffects = new HashMap<>();
        for (Map.Entry<EffectField, PotionData> entry : predictedEffects.entrySet()) {
            PotionData original = entry.getValue();
            int newDuration = (int) (original.duration() * stability);
            int newLevel = stability < 0.4f ? Math.max(0, original.level() - 1) : original.level();

            if (newDuration > 0) {
                finalEffects.put(entry.getKey(), new PotionData(newLevel, newDuration));
            }
        }

        // 返回全新的上下文
        return new StarChartContext(
                context.inputs(),
                route,
                interactions,
                stability,
                finalEffects
        );
    }
}
