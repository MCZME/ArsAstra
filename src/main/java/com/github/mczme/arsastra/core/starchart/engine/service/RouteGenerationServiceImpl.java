package com.github.mczme.arsastra.core.starchart.engine.service;

import com.github.mczme.arsastra.core.element.SpecialElement;
import com.github.mczme.arsastra.core.element.profile.ElementProfileManager;
import com.github.mczme.arsastra.core.starchart.StarChart;
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
    public StarChartRoute computeRoute(List<ItemStack> items, Vector2f startPoint, StarChart chart) {
        List<StarChartPath> segments = new ArrayList<>();
        Vector2f currentPos = new Vector2f(startPoint);

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            ElementProfileManager.getInstance().getElementProfile(stack.getItem()).ifPresent(profile -> {
                List<StarChartPath> rawPaths = generatePathsForItem(profile);
                
                for (StarChartPath rawPath : rawPaths) {
                    StarChartPath pendingRawPath = rawPath;
                    
                    // 循环处理，直到当前这一段相对路径被完全消耗
                    while (pendingRawPath != null && pendingRawPath.getLength() > 0.001f) {
                        // 1. 检查起点是否已经在环境内
                        Environment activeEnv = findEnvironmentAt(currentPos, chart);
                        
                        if (activeEnv != null) {
                            // 情况 A: 起点在环境内 -> 完全委托环境处理
                            // 注意：这里假设环境会“消费”掉传入的整段 pendingRawPath。
                            // 如果环境（如阻力区）让路径变短，那也是一种“消费”（即同样的能量只能走这么远）。
                            List<StarChartPath> processed = activeEnv.getType().processSegment(currentPos, pendingRawPath, activeEnv.shape());
                            segments.addAll(processed);
                            
                            if (!processed.isEmpty()) {
                                currentPos.set(processed.getLast().getEndPoint());
                            }
                            pendingRawPath = null; // 标记为已处理完
                        } else {
                            // 情况 B: 起点不在环境内 -> 寻找最近的进入点
                            float minDist = Float.MAX_VALUE;
                            Environment nearestEnv = null;
                            
                            for (Environment env : chart.environments()) {
                                // 传入 currentPos 作为 offset，因为 pendingRawPath 是相对坐标
                                float d = pendingRawPath.intersect(env.shape(), currentPos);
                                if (d >= 0 && d < minDist) {
                                    minDist = d;
                                    nearestEnv = env;
                                }
                            }
                            
                            if (nearestEnv != null && minDist < pendingRawPath.getLength()) {
                                // B1: 中途撞上了环境 -> 切割
                                StarChartPath[] parts = pendingRawPath.split(minDist);
                                StarChartPath preSegment = parts[0];
                                StarChartPath postSegment = parts[1];
                                
                                // 前半段：安全通过，直接平移
                                if (preSegment != null && preSegment.getLength() > 0.001f) {
                                    StarChartPath absPre = preSegment.offset(currentPos);
                                    segments.add(absPre);
                                    currentPos.set(absPre.getEndPoint());
                                }
                                
                                // 后半段：留给下一轮循环处理 (届时起点将在环境边界上)
                                pendingRawPath = postSegment;
                                
                            } else {
                                // B2: 全程无阻 -> 全部平移
                                StarChartPath absPath = pendingRawPath.offset(currentPos);
                                segments.add(absPath);
                                currentPos.set(absPath.getEndPoint());
                                pendingRawPath = null;
                            }
                        }
                    }
                }
            });
        }

        return new StarChartRoute(segments);
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
