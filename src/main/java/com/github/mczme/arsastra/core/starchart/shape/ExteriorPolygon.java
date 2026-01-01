package com.github.mczme.arsastra.core.starchart.shape;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2f;

import java.util.List;

public record ExteriorPolygon(List<Vector2f> vertices) implements Shape {

    public static final MapCodec<ExteriorPolygon> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtils.VECTOR2F.listOf().fieldOf("vertices").forGetter(ExteriorPolygon::vertices)
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
    public ShapeType getType() {
        return ShapeType.EXTERIOR_POLYGON;
    }
}
