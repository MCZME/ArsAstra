package com.github.mczme.arsastra.core.starchart.environment;

import com.github.mczme.arsastra.core.starchart.shape.Shape;
import com.github.mczme.arsastra.registry.AARegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;


public record Environment(
        String id,
        ResourceLocation type,
        Shape shape
) {
    private static final Codec<ResourceLocation> ENVIRONMENT_TYPE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> AARegistries.ENVIRONMENT_TYPE_REGISTRY.containsKey(id)
                    ? DataResult.success(id)
                    : DataResult.error(() -> "Unknown environment type: " + id),
            DataResult::success
    );

    public static final Codec<Environment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Environment::id),
            ENVIRONMENT_TYPE_CODEC.fieldOf("type").forGetter(Environment::type),
            Shape.CODEC.fieldOf("shape").forGetter(Environment::shape)
    ).apply(instance, Environment::new));

    public EnvironmentType getType() {
        return AARegistries.ENVIRONMENT_TYPE_REGISTRY.get(type);
    }
}
