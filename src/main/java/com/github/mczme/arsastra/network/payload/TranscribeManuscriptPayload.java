package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.manuscript.ClientManuscript;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端请求将一份手稿数据誊录为实体物品。
 */
public record TranscribeManuscriptPayload(ClientManuscript manuscript) implements CustomPacketPayload {
    public static final Type<TranscribeManuscriptPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "transcribe_manuscript"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TranscribeManuscriptPayload> STREAM_CODEC = StreamCodec.composite(
            ClientManuscript.STREAM_CODEC,
            TranscribeManuscriptPayload::manuscript,
            TranscribeManuscriptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
