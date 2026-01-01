package com.github.mczme.arsastra.core.element.profile.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record TemplateDefinition(
        Optional<PartialProfile> data,
        List<TemplateApplyEntry> apply
) implements IElementDefinition {

    public static final MapCodec<TemplateDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PartialProfile.CODEC.optionalFieldOf("data").forGetter(TemplateDefinition::data),
            TemplateApplyEntry.CODEC.listOf().fieldOf("apply").forGetter(TemplateDefinition::apply)
    ).apply(instance, TemplateDefinition::new));

    @Override
    public DefinitionType type() {
        return DefinitionType.TEMPLATE;
    }
}
