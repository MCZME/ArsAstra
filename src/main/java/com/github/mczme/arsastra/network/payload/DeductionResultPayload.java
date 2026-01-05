package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;

import java.util.List;

public record DeductionResultPayload(List<Vector2f> points, float stability) implements CustomPacketPayload {
    public static final Type<DeductionResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "deduction_result"));

    private static final StreamCodec<FriendlyByteBuf, Vector2f> VECTOR2F_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeFloat(v.x);
                buf.writeFloat(v.y);
            },
            (buf) -> new Vector2f(buf.readFloat(), buf.readFloat())
    );

    public static final StreamCodec<FriendlyByteBuf, DeductionResultPayload> STREAM_CODEC = StreamCodec.composite(
            VECTOR2F_CODEC.apply(ByteBufCodecs.list()),
            DeductionResultPayload::points,
            ByteBufCodecs.FLOAT,
            DeductionResultPayload::stability,
            DeductionResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
