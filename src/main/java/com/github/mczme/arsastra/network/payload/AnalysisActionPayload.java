package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record AnalysisActionPayload(BlockPos pos, Action action, Map<ResourceLocation, GuessData> guesses) implements CustomPacketPayload {
    public static final Type<AnalysisActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "analysis_action"));

    public static final StreamCodec<FriendlyByteBuf, AnalysisActionPayload> STREAM_CODEC = StreamCodec.ofMember(
            AnalysisActionPayload::write,
            AnalysisActionPayload::new
    );

    public AnalysisActionPayload(FriendlyByteBuf buffer) {
        this(
            buffer.readBlockPos(),
            buffer.readEnum(Action.class),
            readGuesses(buffer)
        );
    }

    private static Map<ResourceLocation, GuessData> readGuesses(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, GuessData> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation key = buffer.readResourceLocation();
            int value = buffer.readVarInt();
            boolean isPrecise = buffer.readBoolean();
            map.put(key, new GuessData(value, isPrecise));
        }
        return map;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(action);
        buffer.writeVarInt(guesses.size());
        guesses.forEach((key, data) -> {
            buffer.writeResourceLocation(key);
            buffer.writeVarInt(data.value);
            buffer.writeBoolean(data.isPrecise);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        DIRECT_ANALYSIS,
        START_GUESS,
        SUBMIT_GUESS
    }

    public record GuessData(int value, boolean isPrecise) {}
}
