package com.github.mczme.arsastra.core.element.profile;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs; // Added import
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public record ElementProfile(
        Vector2f launchPoint,
        Map<ResourceLocation, Float> elements
) {
    public static final Codec<ElementProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.VECTOR2F_CODEC.fieldOf("launch_point").forGetter(ElementProfile::launchPoint),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("elements").forGetter(ElementProfile::elements)
    ).apply(instance, ElementProfile::new));

    public static final StreamCodec<ByteBuf, ElementProfile> STREAM_CODEC = StreamCodec.composite(
            CodecUtils.VECTOR2F_STREAM_CODEC, ElementProfile::launchPoint,
            ByteBufCodecs.map((IntFunction<Map<ResourceLocation, Float>>) HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT), ElementProfile::elements,
            ElementProfile::new
    );
}
