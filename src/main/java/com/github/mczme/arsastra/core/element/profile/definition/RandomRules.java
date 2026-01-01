package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record RandomRules(
        Optional<List<Float>> totalValueRange,
        Optional<Integer> maxElementTypes,
        Optional<List<ElementPoolEntry>> pool
) {
    public static final Codec<RandomRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.listOf().optionalFieldOf("total_value_range").forGetter(RandomRules::totalValueRange),
            Codec.INT.optionalFieldOf("max_element_types").forGetter(RandomRules::maxElementTypes),
            ElementPoolEntry.CODEC.listOf().optionalFieldOf("pool").forGetter(RandomRules::pool)
    ).apply(instance, RandomRules::new));
}
