package com.github.mczme.arsastra.core.manuscript;

import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 代表客户端存储的一份手稿数据。
 */
public record ClientManuscript(
        String name,
        String icon,
        long createdAt,
        ResourceLocation chart,
        float decayFactor,
        List<AlchemyInput> inputs,
        List<ResourceLocation> effectIds
) {
    public static final Codec<ClientManuscript> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ClientManuscript::name),
            Codec.STRING.optionalFieldOf("icon", "0").forGetter(ClientManuscript::icon),
            Codec.LONG.fieldOf("created_at").forGetter(ClientManuscript::createdAt),
            ResourceLocation.CODEC.fieldOf("chart").forGetter(ClientManuscript::chart),
            Codec.FLOAT.optionalFieldOf("decay_factor", 1.0f).forGetter(ClientManuscript::decayFactor),
            AlchemyInput.CODEC.listOf().fieldOf("inputs").forGetter(ClientManuscript::inputs),
            ResourceLocation.CODEC.listOf().optionalFieldOf("effect_ids", List.of()).forGetter(ClientManuscript::effectIds)
    ).apply(instance, ClientManuscript::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientManuscript> STREAM_CODEC = StreamCodec.of(
            ClientManuscript::write, ClientManuscript::read
    );

    private static void write(RegistryFriendlyByteBuf buf, ClientManuscript manuscript) {
        ByteBufCodecs.STRING_UTF8.encode(buf, manuscript.name);
        ByteBufCodecs.STRING_UTF8.encode(buf, manuscript.icon);
        ByteBufCodecs.VAR_LONG.encode(buf, manuscript.createdAt);
        ResourceLocation.STREAM_CODEC.encode(buf, manuscript.chart);
        ByteBufCodecs.FLOAT.encode(buf, manuscript.decayFactor);
        AlchemyInput.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, manuscript.inputs);
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, manuscript.effectIds);
    }

    private static ClientManuscript read(RegistryFriendlyByteBuf buf) {
        String name = ByteBufCodecs.STRING_UTF8.decode(buf);
        String icon = ByteBufCodecs.STRING_UTF8.decode(buf);
        long createdAt = ByteBufCodecs.VAR_LONG.decode(buf);
        ResourceLocation chart = ResourceLocation.STREAM_CODEC.decode(buf);
        float decayFactor = ByteBufCodecs.FLOAT.decode(buf);
        List<AlchemyInput> inputs = AlchemyInput.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        List<ResourceLocation> effectIds = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
        return new ClientManuscript(name, icon, createdAt, chart, decayFactor, inputs, effectIds);
    }

    /**
     * 检查玩家是否拥有使用该手稿所需的知识。
     * @return 如果缺少知识，返回包含错误信息的 Optional；如果检查通过，返回 Optional.empty()。
     */
    public Optional<Component> checkKnowledge(PlayerKnowledge knowledge) {
        // 1. 检查星图知识
        if (!knowledge.hasVisitedStarChart(this.chart)) {
            return Optional.of(Component.translatable("gui.ars_astra.manuscript.error.unknown_chart", this.chart.getPath()));
        }

        // 2. 检查物品知识
        for (AlchemyInput input : this.inputs) {
            if (!knowledge.hasAnalyzed(input.stack().getItem())) {
                return Optional.of(Component.translatable("gui.ars_astra.manuscript.error.unknown_item", input.stack().getHoverName()));
            }
        }

        return Optional.empty();
    }
}