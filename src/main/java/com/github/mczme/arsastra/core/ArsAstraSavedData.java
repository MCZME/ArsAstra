package com.github.mczme.arsastra.core;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Random;

public class ArsAstraSavedData extends SavedData {

    private static final String DATA_NAME = ArsAstra.MODID;
    private long elementProfileSeed;

    public ArsAstraSavedData() {
        // 第一次创建时，生成一个随机种子
        this.elementProfileSeed = new Random().nextLong();
        ArsAstra.LOGGER.info("New Ars Astra saved data created with element profile seed: {}", this.elementProfileSeed);
        setDirty();
    }

    private ArsAstraSavedData(long seed) {
        this.elementProfileSeed = seed;
    }

    public long getElementProfileSeed() {
        return elementProfileSeed;
    }

    public static ArsAstraSavedData load(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
        long seed = nbt.getLong("elementProfileSeed");
        ArsAstra.LOGGER.debug("Loaded Ars Astra saved data with element profile seed: {}", seed);
        return new ArsAstraSavedData(seed);
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag, HolderLookup.Provider pRegistries) {
        pCompoundTag.putLong("elementProfileSeed", this.elementProfileSeed);
        return pCompoundTag;
    }


    /**
     * 获取当前世界的 ArsAstraSavedData 实例。
     * 如果不存在，则会创建一个新的实例。
     * @param level 服务器端的世界实例
     * @return ArsAstraSavedData 实例
     */
    public static ArsAstraSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new Factory<>(ArsAstraSavedData::new, ArsAstraSavedData::load), DATA_NAME);
    }
}
