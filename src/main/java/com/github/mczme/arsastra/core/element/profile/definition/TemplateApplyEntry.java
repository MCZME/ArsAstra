package com.github.mczme.arsastra.core.element.profile.definition;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.Map;
import java.util.Optional;

public record TemplateApplyEntry(
        Optional<ResourceLocation> item,
        Optional<ResourceLocation> tag,
        Optional<Vector2f> launchPoint,
        Map<ResourceLocation, Float> elements
) {
    public static final Codec<TemplateApplyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(TemplateApplyEntry::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(TemplateApplyEntry::tag),
            CodecUtils.VECTOR2F_CODEC.optionalFieldOf("launch_point").forGetter(TemplateApplyEntry::launchPoint),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("elements").forGetter(TemplateApplyEntry::elements)
    ).apply(instance, TemplateApplyEntry::new));
}
