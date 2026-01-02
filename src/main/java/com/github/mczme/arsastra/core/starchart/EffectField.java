package com.github.mczme.arsastra.core.starchart;

import com.github.mczme.arsastra.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.joml.Vector2f;

public record EffectField(
        ResourceLocation effect,
        Vector2f center,
        int maxLevel
) {
    private static final Codec<ResourceLocation> MOB_EFFECT_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> BuiltInRegistries.MOB_EFFECT.containsKey(id)
                    ? DataResult.success(id)
                    : DataResult.error(() -> "Unknown mob effect: " + id),
            DataResult::success
    );

    public static final Codec<EffectField> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MOB_EFFECT_CODEC.fieldOf("effect").forGetter(EffectField::effect),
            CodecUtils.VECTOR2F_CODEC.fieldOf("center").forGetter(EffectField::center),
            Codec.INT.fieldOf("max_level").forGetter(EffectField::maxLevel)
    ).apply(instance, EffectField::new));

    public MobEffect getEffect() {
        return BuiltInRegistries.MOB_EFFECT.get(effect);
    }

    public float getRadius() {
        return (float) maxLevel * 20.0f;
    }
}
