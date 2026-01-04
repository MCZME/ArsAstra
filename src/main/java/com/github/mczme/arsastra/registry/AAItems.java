package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.item.StarChartJournalItem;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AAItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArsAstra.MODID);
    public static final List<Supplier<Item>> CREATIVE_TAB_ITEMS = new ArrayList<>();

    public static final Supplier<Item> DATA_DRIVEN_ITEM = register("data_driven_item",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> STAR_CHART_JOURNAL = register("star_chart_journal",
            () -> new StarChartJournalItem(new Item.Properties().stacksTo(1).component(AAComponents.IS_OPEN, false)));

    private static Supplier<Item> register(String name, Supplier<Item> supplier) {
        Supplier<Item> registeredItem = ITEMS.register(name, supplier);
        CREATIVE_TAB_ITEMS.add(registeredItem);
        return registeredItem;
    }

    public static List<Supplier<Item>> getCreativeTabItems() {
        return CREATIVE_TAB_ITEMS;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
