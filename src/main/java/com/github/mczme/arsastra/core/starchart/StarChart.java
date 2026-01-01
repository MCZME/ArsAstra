package com.github.mczme.arsastra.core.starchart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StarChart(
        ResourceLocation baseLiquid,
        List<EffectField> fields,
        List<Environment> environments
) {
    public static final Codec<StarChart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("base_liquid").forGetter(StarChart::baseLiquid),
            EffectField.CODEC.listOf().fieldOf("fields").forGetter(StarChart::fields),
            Environment.CODEC.listOf().fieldOf("environments").forGetter(StarChart::environments)
    ).apply(instance, StarChart::new));
}
