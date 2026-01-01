package com.github.mczme.arsastra.core.starchart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.joml.Vector2f;
import com.github.mczme.arsastra.util.CodecUtils;

public record EffectField(
        ResourceLocation effectId,
        Vector2f center,
        int maxLevel
) {
    public static final Codec<EffectField> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect_id").forGetter(EffectField::effectId),
            CodecUtils.VECTOR2F.fieldOf("center").forGetter(EffectField::center),
            Codec.INT.fieldOf("max_level").forGetter(EffectField::maxLevel)
    ).apply(instance, EffectField::new));

    public MobEffect getEffect() {
        return BuiltInRegistries.MOB_EFFECT.get(effectId);
    }
}
