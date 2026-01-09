package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.PotionData;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import org.joml.Vector2f;

import java.util.List;
import java.util.Map;

public class DeductionServiceImpl implements DeductionService {

    private final RouteGenerationService routeService;
    private final InteractionService interactionService;
    private final StabilityService stabilityService;

    public DeductionServiceImpl() {
        this.routeService = new RouteGenerationServiceImpl();
        this.interactionService = new InteractionServiceImpl();
        this.stabilityService = new StabilityServiceImpl();
    }

    @Override
    public DeductionResult deduce(StarChart chart, List<AlchemyInput> inputs, Vector2f startPoint) {
        // 1. 生成完整航线 (几何路径)
        StarChartRoute route = routeService.computeRoute(inputs, startPoint, chart);

        // 2. 计算几何交互结果
        List<InteractionResult> interactions = interactionService.computeInteractions(route, chart);

        // 3. 计算最终药水效果
        Map<EffectField, PotionData> predictedEffects = interactionService.calculateEffects(interactions);

        // 4. 计算稳定性
        float finalStability = stabilityService.computeStability(inputs);

        return new DeductionResult(route, finalStability, predictedEffects);
    }
}
