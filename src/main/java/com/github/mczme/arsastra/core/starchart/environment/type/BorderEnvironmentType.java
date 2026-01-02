package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.core.starchart.shape.Shape;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class BorderEnvironmentType implements EnvironmentType {

    // 基础效率 (20%)，即阻力削减 80%
    private static final float BASE_EFFICIENCY = 0.2f;
    private static final float MIN_EFFICIENCY = 0.1f;

    // 梯度参数：从半径 200 开始是基础阻力，到半径 500 达到最大阻力。
    private static final float GRADIENT_START = 20.0f;
    private static final float GRADIENT_END = 500.0f;

    @Override
    public List<StarChartPath> processSegment(Vector2f currentStart, StarChartPath originalPath, Shape shape) {
        float totalOriginalLength = originalPath.getLength();
        
        // 步长太小会影响性能，太大影响积分精度。取 5.0f 较为适中。
        float stepSize = 5.0f; 
        float effectiveLength = 0.0f;

        Vector2f probe = new Vector2f();

        // 积分计算：累加每一小段的实际行进距离
        for (float d = 0; d < totalOriginalLength; d += stepSize) {
            float currentStep = Math.min(stepSize, totalOriginalLength - d);

            // 计算当前点的绝对位置，用于查询梯度
            // 注意：这里使用的是 originalPath (相对路径) + currentStart (偏移)
            originalPath.getPointAtDistance(d).add(currentStart, probe);

            // 计算当前位置的效率
            float distFromCenter = probe.length();
            float efficiency = calculateEfficiency(distFromCenter);

            effectiveLength += currentStep * efficiency;
        }

        // 如果计算出的实际能走的距离极短，直接返回空
        if (effectiveLength < 0.01f) {
            return Collections.emptyList();
        }

        // 从原路径中截取 effectiveLength 长度的前半段
        // 注意：这里的 split 是在相对路径上进行的
        StarChartPath[] parts = originalPath.split(effectiveLength);
        
        // 将截取出的前半段平移到当前位置，作为最终结果
        // 后半段被丢弃（代表被阻力消耗掉的能量）
        return Collections.singletonList(parts[0].offset(currentStart));
    }

    private float calculateEfficiency(float distFromCenter) {
        if (distFromCenter <= GRADIENT_START) return BASE_EFFICIENCY;
        if (distFromCenter >= GRADIENT_END) return MIN_EFFICIENCY;

        float progress = (distFromCenter - GRADIENT_START) / (GRADIENT_END - GRADIENT_START);
        // 线性插值：距离越远，效率越低 (BASE -> MIN)
        return BASE_EFFICIENCY + (MIN_EFFICIENCY - BASE_EFFICIENCY) * progress;
    }
}
