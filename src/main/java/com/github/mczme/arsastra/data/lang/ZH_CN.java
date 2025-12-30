package com.github.mczme.arsastra.data.lang;

import com.github.mczme.arsastra.ArsAstra;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ZH_CN extends LanguageProvider  {

    public ZH_CN(PackOutput output) {
        super(output, ArsAstra.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ArsAstra.MODID + ".ars_astra_tab", "星枢万象");
    }

}
