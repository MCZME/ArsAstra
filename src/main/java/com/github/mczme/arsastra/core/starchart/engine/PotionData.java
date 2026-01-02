package com.github.mczme.arsastra.core.starchart.engine;

/**
 * 存储单个效果的等级和时长。
 *
 * @param level 药水等级 (Amplifier, 0 = Level 1)
 * @param duration 药水时长 (Ticks)
 */
public record PotionData(int level, int duration) {
    public static final PotionData EMPTY = new PotionData(0, 0);

    public PotionData merge(PotionData other) {
        return new PotionData(
                Math.max(this.level, other.level),
                this.duration + other.duration
        );
    }
}
