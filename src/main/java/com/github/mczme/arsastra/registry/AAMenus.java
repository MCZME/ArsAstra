package com.github.mczme.arsastra.registry;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.menu.AnalysisDeskMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class AAMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ArsAstra.MODID);

    public static final Supplier<MenuType<AnalysisDeskMenu>> ANALYSIS_DESK =
            MENUS.register("analysis_desk",
                    () -> IMenuTypeExtension.create(AnalysisDeskMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}