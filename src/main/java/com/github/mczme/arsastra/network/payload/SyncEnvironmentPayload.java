package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncEnvironmentPayload(String seedHash) implements CustomPacketPayload {
    public static final Type<SyncEnvironmentPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "sync_environment"));

    public static final StreamCodec<FriendlyByteBuf, SyncEnvironmentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SyncEnvironmentPayload::seedHash,
            SyncEnvironmentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
