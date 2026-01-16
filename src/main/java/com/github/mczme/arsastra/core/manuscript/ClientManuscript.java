package com.github.mczme.arsastra.core.manuscript;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 代表客户端存储的一份手稿数据。
 */
public record ClientManuscript(
        String name,
        String icon,
        long createdAt,
        ResourceLocation chart,
        List<AlchemyInput> inputs,
        List<ResourceLocation> effectIds
) {
    public static final Codec<ClientManuscript> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ClientManuscript::name),
            Codec.STRING.optionalFieldOf("icon", "0").forGetter(ClientManuscript::icon),
            Codec.LONG.fieldOf("created_at").forGetter(ClientManuscript::createdAt),
            ResourceLocation.CODEC.fieldOf("chart").forGetter(ClientManuscript::chart),
            AlchemyInput.CODEC.listOf().fieldOf("inputs").forGetter(ClientManuscript::inputs),
            ResourceLocation.CODEC.listOf().optionalFieldOf("effect_ids", List.of()).forGetter(ClientManuscript::effectIds)
    ).apply(instance, ClientManuscript::new));
}