package com.github.mczme.arsastra.core.manuscript;

import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
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