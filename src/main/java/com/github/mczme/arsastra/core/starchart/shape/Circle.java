package com.github.mczme.arsastra.core.starchart.shape;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2f;

public record Circle(Vector2f center, float radius) implements Shape {

    public static final MapCodec<Circle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtils.VECTOR2F_CODEC.fieldOf("center").forGetter(Circle::center),
            Codec.FLOAT.fieldOf("radius").forGetter(Circle::radius)
    ).apply(instance, Circle::new));

    @Override
    public boolean contains(Vector2f point) {
        return point.distanceSquared(center) <= radius * radius;
    }

    @Override
    public ShapeType getType() {
        return ShapeType.CIRCLE;
    }
}
