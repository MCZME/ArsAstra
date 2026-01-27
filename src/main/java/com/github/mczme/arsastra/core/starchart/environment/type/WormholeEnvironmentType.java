package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WormholeEnvironmentType implements EnvironmentType {

    @Override
    public List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        // 虫洞直接吞噬进入的路径，不保留原始路径的任何部分
        
        Map<String, String> data = environment.data();
        String targetId = data.get("target_wormhole_id");
        
        if (targetId == null || targetId.isEmpty()) {
            return Collections.emptyList();
        }

        // 查找目标虫洞
        Environment targetEnv = null;
        for (Environment env : chart.environments()) {
            if (env.id().equals(targetId)) {
                targetEnv = env;
                break;
            }
        }

        if (targetEnv == null) {
            return Collections.emptyList();
        }

        // 读取出口配置 (使用矢量表示)
        float exitX = 50.0f;
        float exitY = 0.0f;
        try {
            if (data.containsKey("exit_x")) exitX = Float.parseFloat(data.get("exit_x"));
            if (data.containsKey("exit_y")) exitY = Float.parseFloat(data.get("exit_y"));
        } catch (NumberFormatException ignored) {}

        Vector2f exitVector = new Vector2f(exitX, exitY);

        // 新路径起点为目标虫洞的中心
        Vector2f exitStart = targetEnv.shape().getCenter();

        // 生成新路径
        LinearStarChartPath newPath = new LinearStarChartPath(new Vector2f(0, 0), exitVector); // 相对
        
        return Collections.singletonList(newPath.offset(exitStart));
    }
}
