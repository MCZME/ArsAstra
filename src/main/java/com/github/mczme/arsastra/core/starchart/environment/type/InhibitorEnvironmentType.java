package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class InhibitorEnvironmentType implements EnvironmentType {

    // 效率系数：0.6 表示只保留 60% 的长度（即 40% 的阻力）
    private static final float EFFICIENCY = 0.6f;

    @Override
    public List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        float totalLength = originalPath.getLength();
        
        // 如果路径极短，直接返回原样平移后的结果
        if (totalLength < 0.01f) {
            return Collections.singletonList(originalPath.offset(currentStart));
        }

        float effectiveLength = totalLength * EFFICIENCY;

        // 使用 split 截取前半段
        // originalPath 是相对路径，split 返回的也是相对路径
        StarChartPath[] parts = originalPath.split(effectiveLength);
        
        // 返回 offset 后的前半段
        // 后半段被丢弃（模拟被阻力消耗掉的动能）
        return Collections.singletonList(parts[0].offset(currentStart));
    }
}
