package com.github.mczme.arsastra.data;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.data.provider.ElementProfileProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.joml.Vector2f;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AAElementProfile extends ElementProfileProvider {

    public AAElementProfile(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, existingFileHelper);
    }

    @Override
    protected void gather() {
        basic("basic_items", 1000, basicBuilder()
                .add(ResourceLocation.withDefaultNamespace("diamond"), new Vector2f(512.0f, 1286.0f), Map.of(
                        ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "order"), 20.0f
                ))
        );
    }
}
