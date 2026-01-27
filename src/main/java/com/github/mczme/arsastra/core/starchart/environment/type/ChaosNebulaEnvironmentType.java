package com.github.mczme.arsastra.core.starchart.environment.type;

import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.environment.EnvironmentType;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import net.minecraft.util.RandomSource;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class ChaosNebulaEnvironmentType implements EnvironmentType {

    private static final float DEFAULT_INTENSITY = 15.0f; // 默认最大旋转角度 +/- 15度

    @Override
    public List<StarChartPath> processSegment(StarChart chart, Vector2f currentStart, StarChartPath originalPath, Environment environment) {
        // 1. 获取混沌强度配置
        float intensity = DEFAULT_INTENSITY;
        if (environment.data().containsKey("chaos_intensity")) {
            try {
                intensity = Float.parseFloat(environment.data().get("chaos_intensity"));
            } catch (NumberFormatException ignored) {}
        }

        // 2. 确定性随机生成
        // 使用当前坐标的 hash 作为种子，确保同一路径在同一位置的表现是一致的 (Seeded Random)
        long seed = Float.floatToIntBits(currentStart.x) ^ Float.floatToIntBits(currentStart.y);
        RandomSource random = RandomSource.create(seed);

        // 3. 计算旋转角度
        // 生成 [-intensity, +intensity] 之间的随机角度
        float rotationDeg = (random.nextFloat() - 0.5f) * 2 * intensity;

        // 4. 应用旋转
        // 将整段路径进行旋转。
        // 注意：offset 必须在 rotate 之后调用，因为 rotate 是基于 (0,0) 的相对变换
        // LinearStarChartPath.rotate 接受的是角度(Degrees)
        return Collections.singletonList(originalPath.rotate(rotationDeg).offset(currentStart));
    }
}
