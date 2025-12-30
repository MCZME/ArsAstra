package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AACreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsAstra.MODID);

    public static final Supplier<CreativeModeTab> ARS_ASTRA_TAB = CREATIVE_MODE_TABS.register("ars_astra_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(Items.STONE))
            .title(Component.translatable("itemGroup." + ArsAstra.MODID + ".ars_astra_tab"))
            .displayItems((displayParameters, output) -> {
                AAItems.getCreativeTabItems().forEach(item -> output.accept(item.get()));
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
