package com.github.mczme.arsastra.core.element.profile.definition;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.Map;
import java.util.Optional;

public record PartialProfile(
        Optional<Vector2f> launchPoint,
        Optional<Map<ResourceLocation, Float>> elements
) {
    public static final Codec<PartialProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.VECTOR2F_CODEC.optionalFieldOf("launch_point").forGetter(PartialProfile::launchPoint),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).optionalFieldOf("elements").forGetter(PartialProfile::elements)
    ).apply(instance, PartialProfile::new));
}
