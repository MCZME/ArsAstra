package com.github.mczme.arsastra.core.starchart.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import org.joml.Vector2f;

public interface Shape {

    Codec<Shape> CODEC = StringRepresentable.fromEnum(ShapeType::values)
            .dispatch(Shape::getType, ShapeType::getCodec);

    boolean contains(Vector2f point);

    ShapeType getType();

    enum ShapeType implements StringRepresentable {
        CIRCLE("circle", Circle.CODEC),
        RECTANGLE("rectangle", Rectangle.CODEC),
        POLYGON("polygon", Polygon.CODEC),
        EXTERIOR_POLYGON("exterior_polygon", ExteriorPolygon.CODEC);

        private final String name;
        private final MapCodec<? extends Shape> codec;

        ShapeType(String name, MapCodec<? extends Shape> codec) {
            this.name = name;
            this.codec = codec;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public MapCodec<? extends Shape> getCodec() {
            return this.codec;
        }
    }
}
