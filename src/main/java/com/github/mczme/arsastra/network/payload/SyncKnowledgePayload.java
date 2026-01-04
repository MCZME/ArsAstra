package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncKnowledgePayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncKnowledgePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "sync_knowledge"));

    public static final StreamCodec<FriendlyByteBuf, SyncKnowledgePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.data()),
            (buf) -> new SyncKnowledgePayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
