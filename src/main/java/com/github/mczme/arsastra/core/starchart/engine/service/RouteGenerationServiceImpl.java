package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.element.SpecialElement;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.InteractionResult;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.registry.AARegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteGenerationServiceImpl implements RouteGenerationService {

    @Override
    public StarChartRoute computeRoute(List<AlchemyInput> inputs, Vector2f startPoint, StarChart chart) {
        List<SegmentData> segmented = computeSegmentedRoute(inputs, startPoint, chart);
        List<StarChartPath> paths = segmented.stream().map(SegmentData::path).toList();
        return new StarChartRoute(paths);
    }

    @Override
    public List<SegmentData> computeSegmentedRoute(List<AlchemyInput> inputs, Vector2f startPoint, StarChart chart) {
        List<SegmentData> results = new ArrayList<>();
        Vector2f currentPos = new Vector2f(startPoint);
        InteractionService interactionService = new InteractionServiceImpl();

        for (AlchemyInput input : inputs) {
            ItemStack stack = input.stack();
            if (stack.isEmpty()) continue;

            final float rotation = input.rotation();

            ElementProfileManager.getInstance().getElementProfile(stack.getItem()).ifPresent(profile -> {
                List<StarChartPath> rawPaths = generatePathsForItem(profile);
                
                for (StarChartPath rawPath : rawPaths) {
                    StarChartPath pendingRawPath = rawPath;
                    
                    if (Math.abs(rotation) > 0.0001f) {
                        pendingRawPath = pendingRawPath.rotate(rotation);
                    }
                    
                    while (pendingRawPath != null && pendingRawPath.getLength() > 0.001f) {
                        Environment activeEnv = findEnvironmentAt(currentPos, chart);
                        StarChartPath finalSegment;

                        if (activeEnv != null) {
                            List<StarChartPath> processed = activeEnv.getType().processSegment(currentPos, pendingRawPath, activeEnv.shape());
                            if (processed.isEmpty()) {
                                pendingRawPath = null;
                                continue;
                            }
                            // 假设环境处理后返回的是完整段（或其首段）
                            finalSegment = processed.get(0); // 简化处理，实际上可能产生多段
                            currentPos.set(finalSegment.getEndPoint());
                            pendingRawPath = null;
                        } else {
                            float minDist = Float.MAX_VALUE;
                            Environment nearestEnv = null;
                            
                            for (Environment env : chart.environments()) {
                                float d = pendingRawPath.intersect(env.shape(), currentPos);
                                if (d >= 0 && d < minDist) {
                                    minDist = d;
                                    nearestEnv = env;
                                }
                            }
                            
                            if (nearestEnv != null && minDist < pendingRawPath.getLength()) {
                                StarChartPath[] parts = pendingRawPath.split(minDist);
                                finalSegment = parts[0].offset(currentPos);
                                currentPos.set(finalSegment.getEndPoint());
                                pendingRawPath = parts[1];
                            } else {
                                finalSegment = pendingRawPath.offset(currentPos);
                                currentPos.set(finalSegment.getEndPoint());
                                pendingRawPath = null;
                            }
                        }

                        if (finalSegment != null) {
                            List<InteractionResult> interactions = interactionService.computeInteractionsForSegment(finalSegment, chart);
                            results.add(new SegmentData(finalSegment, interactions, rotation));
                        }
                    }
                }
            });
        }
        return results;
    }

    private Environment findEnvironmentAt(Vector2f pos, StarChart chart) {
        // 简单遍历查找。如果有重叠，取第一个。
        // 根据数据层定义，Environment 包含 Shape
        for (Environment env : chart.environments()) {
            if (env.shape().contains(pos)) {
                return env;
            }
        }
        return null;
    }

    private List<StarChartPath> generatePathsForItem(com.github.mczme.arsastra.core.element.profile.ElementProfile profile) {
        List<StarChartPath> itemPaths = new ArrayList<>();
        Map<ResourceLocation, Float> elements = profile.elements();

        // 1. 分离基础和特殊要素
        Map<ResourceLocation, Float> basicElements = new HashMap<>();
        Map<ResourceLocation, Float> specialElements = new HashMap<>();

        for (Map.Entry<ResourceLocation, Float> entry : elements.entrySet()) {
            AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey()).ifPresent(element -> {
                if (element instanceof SpecialElement) {
                    specialElements.put(entry.getKey(), entry.getValue());
                } else {
                    basicElements.put(entry.getKey(), entry.getValue());
                }
            });
        }

        // 2. 生成基础要素路径
        for (Map.Entry<ResourceLocation, Float> entry : basicElements.entrySet()) {
            AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey()).ifPresent(element -> {
                itemPaths.add(element.getPath(entry.getValue()));
            });
        }

        // 3. 特殊要素塑形
        for (Map.Entry<ResourceLocation, Float> entry : specialElements.entrySet()) {
            AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey()).ifPresent(element -> {
                itemPaths.add(element.getPath(entry.getValue(), basicElements));
            });
        }

        return itemPaths;
    }
}
