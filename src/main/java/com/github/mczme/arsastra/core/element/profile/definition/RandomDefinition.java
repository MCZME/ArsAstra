package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RandomDefinition(
        List<RandomApplyEntry> apply,
        RandomRules rules
) implements IElementDefinition {

    public static final MapCodec<RandomDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RandomApplyEntry.CODEC.listOf().fieldOf("apply").forGetter(RandomDefinition::apply),
            RandomRules.CODEC.fieldOf("rules").forGetter(RandomDefinition::rules)
    ).apply(instance, RandomDefinition::new));

    @Override
    public DefinitionType type() {
        return DefinitionType.RANDOM;
    }
}
