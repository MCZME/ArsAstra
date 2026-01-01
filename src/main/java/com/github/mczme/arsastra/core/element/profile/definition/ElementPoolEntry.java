package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ElementPoolEntry(
        ResourceLocation element,
        int weight
) {
    public static final Codec<ElementPoolEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("element").forGetter(ElementPoolEntry::element),
            Codec.INT.fieldOf("weight").forGetter(ElementPoolEntry::weight)
    ).apply(instance, ElementPoolEntry::new));
}
