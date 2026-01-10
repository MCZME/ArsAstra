package com.github.mczme.arsastra.data.lang;

import java.util.function.Supplier;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.registry.AAElements;
import com.github.mczme.arsastra.registry.AAItems;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ZH_CN extends LanguageProvider  {

    public ZH_CN(PackOutput output) {
        super(output, ArsAstra.MODID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + ArsAstra.MODID + ".ars_astra_tab", "星枢万象");

        // 物品
        addItem(AAItems.STAR_CHART_JOURNAL, "星图日志");

        // GUI 文本
        add("gui.ars_astra.journal.tab.compendium", "要素典籍");
        add("gui.ars_astra.journal.tab.workshop", "推演工坊");
        add("gui.ars_astra.journal.tab.blueprints", "蓝图档案");
        add("gui.ars_astra.journal.tab.atlas", "星图总览");
        add("gui.ars_astra.journal.compendium.elements", "已解锁要素");
        add("gui.ars_astra.journal.compendium.select_element", "请选择一个要素以查看详情");
        add("gui.ars_astra.journal.compendium.search", "搜索档案...");
        // Workshop
        add("gui.ars_astra.workshop.sequence", "推演序列");
        add("gui.ars_astra.workshop.source", "原料来源");
        add("gui.ars_astra.workshop.clear.title", "重构推演");
        add("gui.ars_astra.workshop.clear.confirm", "确认重构");
        add("gui.ars_astra.workshop.remove", "垃圾桶");
        add("gui.ars_astra.workshop.rotation", "旋转角度: %s°");
        
        // 筛选器
        add("gui.ars_astra.filter.elements_hint", "例如：生命, 物质...");
        add("gui.ars_astra.filter.tags_hint", "例如：logs, stones...");
        add("gui.ars_astra.filter.elements_label", "包含要素 (且)");
        add("gui.ars_astra.filter.tags_label", "包含标签 (且)");

        // 元素
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
