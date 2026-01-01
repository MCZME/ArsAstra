package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DefinitionFile(
        int priority,
        IElementDefinition definition
) {
    private static final MapCodec<IElementDefinition> DEFINITION_MAP_CODEC = DefinitionType.CODEC.dispatchMap(
            IElementDefinition::type,
            type -> switch (type) {
                case BASIC -> BasicDefinition.MAP_CODEC;
                case TEMPLATE -> TemplateDefinition.MAP_CODEC;
                case RANDOM -> RandomDefinition.MAP_CODEC;
            }
    );

    public static final Codec<DefinitionFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("priority").forGetter(DefinitionFile::priority),
            DEFINITION_MAP_CODEC.forGetter(DefinitionFile::definition)
    ).apply(instance, DefinitionFile::new));
}
