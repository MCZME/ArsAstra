package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.element.SpecialElement;
import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
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
        List<StarChartPath> results = new ArrayList<>();
        Vector2f currentPos = new Vector2f(startPoint);

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
                            finalSegment = processed.get(0);
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
                            results.add(finalSegment);
                        }
                    }
                }
            });
        }
        return new StarChartRoute(results);
    }

    private Environment findEnvironmentAt(Vector2f pos, StarChart chart) {
        for (Environment env : chart.environments()) {
            if (env.shape().contains(pos)) {
                return env;
            }
        }
        return null;
    }

    private List<StarChartPath> generatePathsForItem(ElementProfile profile) {
        List<StarChartPath> itemPaths = new ArrayList<>();
        Map<ResourceLocation, Float> elements = profile.elements();

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

        if (!specialElements.isEmpty()) {
            // 存在特殊要素：由特殊要素决定路径形态
            // 特殊要素会利用基础要素提供的总能量/方向来“塑形”
            for (Map.Entry<ResourceLocation, Float> entry : specialElements.entrySet()) {
                AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey()).ifPresent(element -> {
                    itemPaths.add(element.getPath(entry.getValue(), basicElements));
                });
            }
        } else {
            // 仅有基础要素：计算矢量和，生成单一直线路径
            Vector2f totalVector = new Vector2f(0, 0);
            for (Map.Entry<ResourceLocation, Float> entry : basicElements.entrySet()) {
                AARegistries.ELEMENT_REGISTRY.getOptional(entry.getKey()).ifPresent(element -> {
                    Vector2f component = new Vector2f(element.getVector()).mul(entry.getValue());
                    totalVector.add(component);
                });
            }

            if (totalVector.lengthSquared() > 0.0001f) {
                itemPaths.add(new LinearStarChartPath(new Vector2f(0, 0), totalVector));
            }
        }

        return itemPaths;
    }
}
