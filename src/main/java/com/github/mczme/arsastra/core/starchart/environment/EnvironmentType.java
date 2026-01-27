package com.github.mczme.arsastra.core.starchart.environment;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.registry.AARegistries;
import net.minecraft.Util;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public interface EnvironmentType {

    default String getDescriptionId() {
        return Util.makeDescriptionId("environmentType", AARegistries.ENVIRONMENT_TYPE_REGISTRY.getKey(this));
    }

    /**
     * 处理一段即将生成的路径。
     * 环境可以决定修改起点、修改路径本身，或插入额外的路径段。
     *
     * @param chart        星图实例 (可用于查找其他环境)
     * @param currentStart 当前游标位置 (绝对坐标)
     * @param originalPath 原始生成的路径段 (相对于 (0,0) 的模板)
     * @param environment  环境实例 (包含形状、配置数据等)
     * @return 处理后的路径段列表 (绝对坐标)。
     */
    default List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        // 默认行为：简单的平移拼接
        return Collections.singletonList(originalPath.offset(currentStart));
    }
}
