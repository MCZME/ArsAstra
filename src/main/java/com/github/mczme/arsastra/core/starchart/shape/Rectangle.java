package com.github.mczme.arsastra.core.starchart.shape;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2f;

public record Rectangle(Vector2f min, Vector2f max) implements Shape {

    public static final MapCodec<Rectangle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CodecUtils.VECTOR2F_CODEC.fieldOf("min").forGetter(Rectangle::min),
            CodecUtils.VECTOR2F_CODEC.fieldOf("max").forGetter(Rectangle::max)
    ).apply(instance, Rectangle::new));

    @Override
    public boolean contains(Vector2f point) {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y;
    }

    @Override
    public Vector2f getCenter() {
        return new Vector2f(min).add(max).div(2);
    }

    @Override
    public float getCharacteristicSize() {
        return min.distance(max);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.RECTANGLE;
    }
}
