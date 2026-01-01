package com.github.mczme.arsastra.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.joml.Vector2f;

import java.util.List;

public class CodecUtils {

    /**
     * A codec for {@link Vector2f}.
     * <p>
     * This codec serializes a {@link Vector2f} to a list of two floats, and deserializes it back.
     * It ensures that the list contains exactly two elements.
     */
    public static final Codec<Vector2f> VECTOR2F = Codec.FLOAT.listOf().comapFlatMap(list -> {
        if (list.size() != 2) {
            return DataResult.error(() -> "Vector2f must have 2 elements, but found " + list.size());
        }
        return DataResult.success(new Vector2f(list.get(0), list.get(1)));
    }, vec -> List.of(vec.x(), vec.y()));

}
