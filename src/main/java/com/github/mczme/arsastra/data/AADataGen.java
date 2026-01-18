package com.github.mczme.arsastra.data;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.data.lang.ZH_CN;
import com.github.mczme.arsastra.data.provider.AABlockStateProvider;
import com.github.mczme.arsastra.data.provider.AABlockTagsProvider;
import com.github.mczme.arsastra.data.provider.AAItemModelProvider;
import com.github.mczme.arsastra.data.provider.AALootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ArsAstra.MODID)
public class AADataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ZH_CN(output));
        generator.addProvider(event.includeClient(), new AABlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new AAItemModelProvider(output, existingFileHelper));
        generator.addProvider(event.includeServer(), new AABlockTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), AALootTableProvider.create(output, lookupProvider));
        generator.addProvider(event.includeServer(), new AAElementProfile(output, lookupProvider, existingFileHelper));
    }
}
