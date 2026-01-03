package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.List;

public class DeductionServiceImpl implements DeductionService {

    private final RouteGenerationService routeService;
    private final StabilityService stabilityService;

    public DeductionServiceImpl() {
        this.routeService = new RouteGenerationServiceImpl();
        this.stabilityService = new StabilityServiceImpl();
    }

    @Override
    public DeductionResult deduce(StarChart chart, List<AlchemyInput> inputs, Vector2f startPoint) {
        // 1. 生成分段路径数据
        List<SegmentData> segments = routeService.computeSegmentedRoute(inputs, startPoint, chart);

        // 2. 为了计算稳定度，我们需要一个完整的 StarChartRoute
        List<StarChartPath> paths = segments.stream().map(SegmentData::path).toList();
        StarChartRoute route = new StarChartRoute(paths);

        // 3. 计算稳定度
        float finalStability = stabilityService.computeStability(inputs);

        // 4. 判定是否成功 (目前简单判定：稳定度大于 0 即为成功)
        boolean isSuccess = finalStability > 0.001f;

        return new DeductionResult(segments, finalStability, isSuccess);
    }
}
