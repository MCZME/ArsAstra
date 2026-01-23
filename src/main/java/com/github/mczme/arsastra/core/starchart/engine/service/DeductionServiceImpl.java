package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
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

        // 4. 计算稳定性 (默认基准系数 1.0)
        float finalStability = stabilityService.computeStability(inputs, 1.0f);
        
        // 5. 应用稳定性修正 (保持与 Engine 一致的预测逻辑)
        Map<EffectField, PotionData> finalEffects = new java.util.HashMap<>();
        for (Map.Entry<EffectField, PotionData> entry : predictedEffects.entrySet()) {
            PotionData original = entry.getValue();
            int newDuration = (int) (original.duration() * finalStability);
            int newLevel = finalStability < 0.4f ? Math.max(0, original.level() - 1) : original.level();
            
            if (newDuration > 0) {
                finalEffects.put(entry.getKey(), new PotionData(newLevel, newDuration));
            }
        }

        return new DeductionResult(route, finalStability, finalEffects);
    }

    @Override
    public DeductionResult deduce(StarChart chart, List<AlchemyInput> inputs) {
        Vector2f startPoint = new Vector2f(0, 0);
        if (!inputs.isEmpty()) {
            AlchemyInput firstInput = inputs.get(0);
            startPoint = ElementProfileManager.getInstance()
                .getElementProfile(firstInput.stack().getItem())
                .map(p -> new Vector2f(p.launchPoint()))
                .orElse(new Vector2f(0, 0));
        }
        return deduce(chart, inputs, startPoint);
    }
}
