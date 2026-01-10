package com.github.mczme.arsastra.core.starchart.path;

import com.github.mczme.arsastra.core.starchart.shape.Shape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * 代表一段在星图上生成的路径。
 * 它可以是线性的，也可以是曲线。
 */
public interface StarChartPath {

    StreamCodec<FriendlyByteBuf, StarChartPath> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public StarChartPath decode(FriendlyByteBuf buf) {
            byte type = buf.readByte();
            if (type == 0) {
                Vector2f start = new Vector2f(buf.readFloat(), buf.readFloat());
                Vector2f end = new Vector2f(buf.readFloat(), buf.readFloat());
                return new LinearStarChartPath(start, end);
            }
            // Future: type 1 for ArcStarChartPath
            throw new UnsupportedOperationException("Unknown path type: " + type);
        }

        @Override
        public void encode(FriendlyByteBuf buf, StarChartPath path) {
            if (path instanceof LinearStarChartPath linear) {
                buf.writeByte(0);
                Vector2f start = linear.getStartPoint();
                Vector2f end = linear.getEndPoint();
                buf.writeFloat(start.x);
                buf.writeFloat(start.y);
                buf.writeFloat(end.x);
                buf.writeFloat(end.y);
            } else {
                throw new UnsupportedOperationException("Unknown path implementation: " + path.getClass());
            }
        }
    };

    /**
     * 获取路径的起始点坐标。
     * @return 起始点坐标
     */
    Vector2f getStartPoint();

    /**
     * 获取路径的终点坐标。
     * @return 终点坐标
     */
    Vector2f getEndPoint();

    /**
     * 获取路径的总长度。
     * @return 路径长度
     */
    float getLength();

    /**
     * 获取路径上从起点出发，行进了`distance`距离后的点的坐标。
     * 这是实现路径与星域交互、渲染等功能的核心。
     * @param distance 从起点出发的距离 (值的范围应在 0 到 getLength() 之间)
     * @return 路径上对应点的坐标
     */
    Vector2f getPointAtDistance(float distance);

    /**
     * 对路径进行采样，返回一系列用于绘制的点。
     * @param stepSize 采样步长
     * @return 采样点列表
     */
    List<Vector2f> sample(float stepSize);

    /**
     * 将路径平移指定的偏移量，并返回一个新的路径对象。
     * @param offset 偏移量
     * @return 平移后的路径
     */
    StarChartPath offset(Vector2f offset);

    /**
     * 以起点为圆心旋转路径指定的角度，并返回一个新的路径对象。
     * @param angle 旋转角度 (弧度)
     * @return 旋转后的路径
     */
    StarChartPath rotate(float angle);

    /**
     * 计算路径与指定形状的第一个交点（进入点）。
     *
     * @param shape  环境形状
     * @param offset 路径的绝对平移量（因为求交是在世界坐标系下进行的）
     * @return 交点处距离起点的距离 distance (0 <= dist <= getLength())。如果不相交，返回 -1.0f。
     */
    default float intersect(Shape shape, Vector2f offset) {
        // 默认实现：步进法 (Step-marching)
        // 精度：1.0 (一个像素/单位)
        float step = 1.0f;
        float length = getLength();
        // 检查起点是否已经在内部（如果已经在内部，则不需要求“进入点”，或者交点就是 0）
        // 这里的逻辑定义为：寻找“穿过边界”的点。
        // 如果起点在内，我们通常寻找“离开点”；但目前逻辑是“寻找进入环境的点”。
        // 如果起点已经在环境内，intersect 应该返回什么？
        // 根据 RouteGenerationService 的逻辑，如果起点在环境内，会直接交给环境处理。
        // 所以调用 intersect 时，前提通常是起点不在环境内。
        
        Vector2f probe = new Vector2f();
        for (float d = 0; d <= length; d += step) {
            getPointAtDistance(d).add(offset, probe);
            if (shape.contains(probe)) {
                return d;
            }
        }
        return -1.0f;
    }

    /**
     * 在指定距离处将路径一分为二。
     *
     * @param distance 分割点距离 (0 < distance < getLength())
     * @return 包含两个新路径的数组：[0]为前半段，[1]为后半段。
     */
    StarChartPath[] split(float distance);
}
