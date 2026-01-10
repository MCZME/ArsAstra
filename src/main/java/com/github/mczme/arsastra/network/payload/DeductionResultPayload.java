package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeductionResultPayload(DeductionResult result) implements CustomPacketPayload {
    public static final Type<DeductionResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "deduction_result"));

    public static final StreamCodec<FriendlyByteBuf, DeductionResultPayload> STREAM_CODEC = StreamCodec.composite(
            DeductionResult.STREAM_CODEC, DeductionResultPayload::result,
            DeductionResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
