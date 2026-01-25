package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class AATags {
    public static class Blocks {
        public static final TagKey<Block> HEAT_SOURCES = tag("heat_sources");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, name));
        }
    }
}
