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

        // 读取出口配置
        float exitAngle = 0.0f;
        try {
            exitAngle = Float.parseFloat(data.getOrDefault("exit_angle", "0"));
        } catch (NumberFormatException ignored) {}

        float exitLength = 50.0f; // 默认长度
        try {
            exitLength = Float.parseFloat(data.getOrDefault("exit_length", "50"));
        } catch (NumberFormatException ignored) {}

        // 计算出口向量
        // 角度 0 对应 X 轴正方向 (1, 0)
        double radians = Math.toRadians(exitAngle);
        Vector2f exitVector = new Vector2f((float) Math.cos(radians), (float) Math.sin(radians)).mul(exitLength);

        // 新路径起点为目标虫洞的中心
        Vector2f exitStart = targetEnv.shape().getCenter();

        // 生成新路径
        // 注意：这里返回的路径是绝对坐标的，但 StarChartPath 通常存储相对矢量
        // LinearStarChartPath 的构造函数是 (Vector2f vector)，表示相对位移
        // 外部 RouteGenerationService 会将 offset(currentPos) 后的终点作为下一次的 currentPos
        // 但 processSegment 的契约是返回 "绝对坐标的路径段" (see interface javadoc: "返回 offset 后的...")
        
        // LinearStarChartPath 默认是相对的 (0,0 -> vector)
        // 我们需要返回 offset 后的实例
        LinearStarChartPath newPath = new LinearStarChartPath(new Vector2f(0, 0), exitVector); // 相对
        
        // 关键点：我们需要告诉引擎，新的起点变成了 exitStart
        // 但 processSegment 的返回值列表会被引擎依次添加到结果中，并更新 currentPos = segment.getEndPoint()
        // 如果我们返回 offset(exitStart) 的路径，引擎会正确记录它。
        // 问题是：当前路径中断在 wormhole A，下一段路径突然出现在 wormhole B。
        // 视觉上这会有一条跳跃线吗？ StarChartRoute 只是 List<Path>。渲染时如果它是断开的，Renderer 需要处理。
        // 目前假设 Renderer 只是画出每一段。
        
        return Collections.singletonList(newPath.offset(exitStart));
    }
}
