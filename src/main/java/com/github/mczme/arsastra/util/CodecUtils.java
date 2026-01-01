package com.github.mczme.arsastra.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2f;

import java.util.List;

public class CodecUtils {
    public static final Codec<Vector2f> VECTOR2F_CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> {
                if (list.size() != 2) {
                    return DataResult.error(() -> "Vector2f must have 2 elements, found " + list.size());
                }
                return DataResult.success(new Vector2f(list.get(0), list.get(1)));
            },
            vec -> List.of(vec.x(), vec.y())
    );

    public static final StreamCodec<ByteBuf, Vector2f> VECTOR2F_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Vector2f::x,
            ByteBufCodecs.FLOAT, Vector2f::y,
            Vector2f::new
    );
}
