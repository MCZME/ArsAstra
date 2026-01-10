package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.engine.AlchemyInput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record RequestDeductionPayload(ResourceLocation starChartId, List<AlchemyInput> inputs) implements CustomPacketPayload {
    public static final Type<RequestDeductionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "request_deduction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestDeductionPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            RequestDeductionPayload::starChartId,
            AlchemyInput.STREAM_CODEC.apply(ByteBufCodecs.list()),
            RequestDeductionPayload::inputs,
            RequestDeductionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
