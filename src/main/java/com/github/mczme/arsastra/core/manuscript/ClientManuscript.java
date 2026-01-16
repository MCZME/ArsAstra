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
        List<String> outcome
) {
    public static final Codec<ClientManuscript> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ClientManuscript::name),
            Codec.STRING.optionalFieldOf("icon", "0").forGetter(ClientManuscript::icon),
            Codec.LONG.fieldOf("created_at").forGetter(ClientManuscript::createdAt),
            ResourceLocation.CODEC.fieldOf("chart").forGetter(ClientManuscript::chart),
            AlchemyInput.CODEC.listOf().fieldOf("inputs").forGetter(ClientManuscript::inputs),
            Codec.STRING.listOf().optionalFieldOf("outcome", List.of()).forGetter(ClientManuscript::outcome)
    ).apply(instance, ClientManuscript::new));
}