package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public record TemplateApplyEntry(
        Optional<ResourceLocation> item,
        Optional<ResourceLocation> tag,
        Map<ResourceLocation, Float> elements
) {
    public static final Codec<TemplateApplyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(TemplateApplyEntry::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(TemplateApplyEntry::tag),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("elements").forGetter(TemplateApplyEntry::elements)
    ).apply(instance, TemplateApplyEntry::new));
}
