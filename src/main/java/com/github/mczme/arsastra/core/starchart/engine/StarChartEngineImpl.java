package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.service.*;
import org.joml.Vector2f;

import java.util.List;
import java.util.Map;

public class StarChartEngineImpl implements StarChartEngine {

    private final RouteGenerationService routeService = new RouteGenerationServiceImpl();
    private final InteractionService interactionService = new InteractionServiceImpl();
    private final StabilityService stabilityService = new StabilityServiceImpl();

    @Override
    public StarChartContext compute(StarChart chart, StarChartContext context, Vector2f startPoint) {
        // 1. 生成路径 (传入的是完整的 context.inputs() 和 当前星图)
        StarChartRoute route = routeService.computeRoute(context.inputs(), startPoint, chart);

        // 2. 计算交互
        List<InteractionResult> interactions = interactionService.computeInteractions(route, chart);

        // 3. 计算稳定性
        float stability = stabilityService.computeStability(context.inputs());

        // 4. 计算最终效果 (PotionData)
        Map<EffectField, PotionData> predictedEffects = interactionService.calculateEffects(interactions);

        // 返回全新的上下文
        return new StarChartContext(
                context.inputs(),
                route,
                interactions,
                stability,
                predictedEffects
        );
    }
}
