package com.github.mczme.arsastra.core.starchart.engine;

import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collections;
import java.util.List;

/**
 * 代表一条完整的星图航线，由多个航段（StarChartPath）组成。
 */
public record StarChartRoute(List<StarChartPath> segments) {
    public static final StarChartRoute EMPTY = new StarChartRoute(Collections.emptyList());

    public static final StreamCodec<FriendlyByteBuf, StarChartRoute> STREAM_CODEC = StreamCodec.composite(
            StarChartPath.STREAM_CODEC.apply(ByteBufCodecs.list()),
            StarChartRoute::segments,
            StarChartRoute::new
    );

    public float getTotalLength() {
        return (float) segments.stream().mapToDouble(StarChartPath::getLength).sum();
    }
}
