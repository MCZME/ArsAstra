package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record RandomApplyEntry(
        Optional<ResourceLocation> item,
        Optional<ResourceLocation> tag
) {
    public static final Codec<RandomApplyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(RandomApplyEntry::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(RandomApplyEntry::tag)
    ).apply(instance, RandomApplyEntry::new));
}
