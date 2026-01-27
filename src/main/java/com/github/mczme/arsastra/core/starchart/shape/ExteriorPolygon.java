package com.github.mczme.arsastra.core.starchart.shape;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2f;

import java.util.List;

public record ExteriorPolygon(List<Vector2f> vertices) implements Shape {

    public static final MapCodec<ExteriorPolygon> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtils.VECTOR2F_CODEC.listOf().fieldOf("vertices").forGetter(ExteriorPolygon::vertices)
    ).apply(instance, ExteriorPolygon::new));

    @Override
    public boolean contains(Vector2f point) {
        int i, j;
        boolean result = false;
        for (i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            if ((vertices.get(i).y > point.y) != (vertices.get(j).y > point.y) &&
                    (point.x < (vertices.get(j).x - vertices.get(i).x) * (point.y - vertices.get(i).y) / (vertices.get(j).y - vertices.get(i).y) + vertices.get(i).x)) {
                result = !result;
            }
        }
        return !result;
    }

    @Override
    public Vector2f getCenter() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Vector2f v : vertices) {
            if (v.x < minX) minX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.x > maxX) maxX = v.x;
            if (v.y > maxY) maxY = v.y;
        }
        return new Vector2f(minX + maxX, minY + maxY).div(2);
    }

    @Override
    public float getCharacteristicSize() {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Vector2f v : vertices) {
            if (v.x < minX) minX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.x > maxX) maxX = v.x;
            if (v.y > maxY) maxY = v.y;
        }
        return new Vector2f(maxX - minX, maxY - minY).length();
    }

    @Override
    public ShapeType getType() {
        return ShapeType.EXTERIOR_POLYGON;
    }
}
