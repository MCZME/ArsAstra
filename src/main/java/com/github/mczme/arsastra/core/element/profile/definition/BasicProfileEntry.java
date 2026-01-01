package com.github.mczme.arsastra.core.element.profile.definition;

import com.github.mczme.arsastra.core.element.profile.ElementProfile;
import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record BasicProfileEntry(
        ResourceLocation item,
        ElementProfile profile
) {
    public static final Codec<BasicProfileEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(BasicProfileEntry::item),
            CodecUtils.VECTOR2F_CODEC.fieldOf("launch_point").forGetter(entry -> entry.profile().launchPoint()),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("elements").forGetter(entry -> entry.profile().elements())
    ).apply(instance, (item, launchPoint, elements) -> {
        return new BasicProfileEntry(item, new ElementProfile(launchPoint, elements));
    }));
}
