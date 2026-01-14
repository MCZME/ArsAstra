package com.github.mczme.arsastra.core.manuscript;

import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * 代表客户端存储的一份手稿数据。
 */
public record ClientManuscript(
        String name,
        long createdAt,
        List<String> tags,
        List<AlchemyInput> inputs
) {
    public static final Codec<ClientManuscript> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ClientManuscript::name),
            Codec.LONG.fieldOf("created_at").forGetter(ClientManuscript::createdAt),
            Codec.STRING.listOf().fieldOf("tags").forGetter(ClientManuscript::tags),
            AlchemyInput.CODEC.listOf().fieldOf("inputs").forGetter(ClientManuscript::inputs)
    ).apply(instance, ClientManuscript::new));
}
