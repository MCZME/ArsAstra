package com.github.mczme.arsastra.core.element.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

public record ElementProfile(
        Optional<Vector2f> launchPoint,
        Map<ResourceLocation, Float> elements
) {
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

    public static final Codec<ElementProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VECTOR2F_CODEC.optionalFieldOf("launch_point").forGetter(ElementProfile::launchPoint),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("elements").forGetter(ElementProfile::elements)
    ).apply(instance, ElementProfile::new));

    public static final StreamCodec<ByteBuf, ElementProfile> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(VECTOR2F_STREAM_CODEC), ElementProfile::launchPoint,
            ByteBufCodecs.map((IntFunction<Map<ResourceLocation, Float>>) HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT), ElementProfile::elements,
            ElementProfile::new
    );
}
