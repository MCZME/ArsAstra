package com.github.mczme.arsastra.network.payload;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record AnalysisActionPayload(BlockPos pos, Action action, Map<ResourceLocation, Integer> guesses) implements CustomPacketPayload {
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

    private static Map<ResourceLocation, Integer> readGuesses(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buffer.readResourceLocation(), buffer.readVarInt());
        }
        return map;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(action);
        buffer.writeVarInt(guesses.size());
        guesses.forEach((key, value) -> {
            buffer.writeResourceLocation(key);
            buffer.writeVarInt(value);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        DIRECT_ANALYSIS, // 学者路线：直接分析
        START_GUESS,     // 直觉路线：开始猜测
        SUBMIT_GUESS,    // 提交猜测数据
        QUIT_GUESS       // 放弃猜测（转为直接分析或退出）
    }
}
