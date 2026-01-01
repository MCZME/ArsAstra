package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record BasicDefinition(
        List<BasicProfileEntry> profiles
) implements IElementDefinition {

    public static final MapCodec<BasicDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BasicProfileEntry.CODEC.listOf().fieldOf("profiles").forGetter(BasicDefinition::profiles)
    ).apply(instance, BasicDefinition::new));

    @Override
    public DefinitionType type() {
        return DefinitionType.BASIC;
    }
}
