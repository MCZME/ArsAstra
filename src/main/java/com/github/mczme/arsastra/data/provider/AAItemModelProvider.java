package com.github.mczme.arsastra.data.provider;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.registry.AAItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class AAItemModelProvider extends ItemModelProvider {
    public AAItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsAstra.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("analysis_desk", modLoc("block/analysis_desk"));
        withExistingParent("copper_tun", modLoc("block/copper_tun"));
        basicItem(AAItems.MANUSCRIPT.get());
    }
}
