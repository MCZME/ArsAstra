package com.github.mczme.arsastra.data.lang;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.registry.AAElements;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.function.Supplier;

public class ZH_CN extends LanguageProvider  {

    public ZH_CN(PackOutput output) {
        super(output, ArsAstra.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ArsAstra.MODID + ".ars_astra_tab", "星枢万象");

        // Elements
        add(AAElements.ORDER, "秩序");
        add(AAElements.DEATH, "死亡");
        add(AAElements.MATTER, "物质");
        add(AAElements.LIFE, "生命");
        add(AAElements.STRUCTURE, "结构");
        add(AAElements.GROWTH, "生长");
        add(AAElements.CORROSION, "腐蚀");
        add(AAElements.DECAY, "凋零");
    }

    private void add(Supplier<? extends Element> key, String name) {
        add(key.get().getDescriptionId(), name);
    }

}
