package com.github.mczme.arsastra.core.starchart;

import com.github.mczme.arsastra.core.starchart.shape.Shape;
import com.github.mczme.arsastra.registry.AARegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import com.github.mczme.arsastra.core.environment.EnvironmentType;


public record Environment(
        String id,
        ResourceLocation typeId,
        Shape shape
) {
    public static final Codec<Environment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Environment::id),
            ResourceLocation.CODEC.fieldOf("type").forGetter(Environment::typeId),
            Shape.CODEC.fieldOf("shape").forGetter(Environment::shape)
    ).apply(instance, Environment::new));

    public EnvironmentType getType() {
        return AARegistries.ENVIRONMENT_TYPE_REGISTRY.get(typeId);
    }
}
