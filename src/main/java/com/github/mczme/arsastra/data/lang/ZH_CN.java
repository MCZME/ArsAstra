package com.github.mczme.arsastra.data.lang;

import java.util.function.Supplier;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.core.element.Element;
import com.github.mczme.arsastra.registry.AABlocks;
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

        // 方块
        addBlock(AABlocks.ANALYSIS_DESK, "分析台");

        // GUI 文本
        add("gui.ars_astra.journal.tab.compendium", "要素典籍");
        add("gui.ars_astra.journal.tab.workshop", "推演工坊");
        add("gui.ars_astra.journal.tab.manuscripts", "手稿档案");
        add("gui.ars_astra.journal.tab.atlas", "星图总览");
        add("gui.ars_astra.journal.compendium.elements", "已解锁要素");
        add("gui.ars_astra.journal.compendium.select_element", "请选择一个要素以查看详情");
        add("gui.ars_astra.journal.compendium.search", "搜索档案...");
        add("gui.ars_astra.compendium.click_to_view", "点击查看详情");
        add("gui.ars_astra.atlas.field.unknown", "未知星域");
        add("gui.ars_astra.atlas.empty", "尚未发现任何星图");
        // Workshop
        add("gui.ars_astra.workshop.sequence", "推演序列");
        add("gui.ars_astra.workshop.source", "原料来源");
        add("gui.ars_astra.workshop.clear.title", "重构推演");
        add("gui.ars_astra.workshop.clear.confirm", "确认重构");
        add("gui.ars_astra.workshop.remove", "丢弃");
        add("gui.ars_astra.workshop.rotation", "旋转角度： %s°");
        add("gui.ars_astra.workshop.save_manuscript.name_hint", "手稿名称...");
        add("gui.ars_astra.workshop.info", "信息");
        
        // Sticky Note (Result Info)
        add("gui.ars_astra.workshop.note", "推演报告");
        add("gui.ars_astra.workshop.no_result", "等待推演...");
        add("gui.ars_astra.workshop.stability", "稳定性");
        add("gui.ars_astra.workshop.effects", "预测产物");
        
        // 手稿详情
        add("gui.ars_astra.load", "加载配置");
        add("gui.ars_astra.delete", "删除手稿");
        add("gui.ars_astra.confirm", "确认删除？");
        add("gui.ars_astra.manuscript.step", "步骤 %d");
        add("gui.ars_astra.manuscript.rotation", "旋转角度: %s°");
        add("gui.ars_astra.manuscript.manage", "管理手稿");
        add("gui.ars_astra.manuscript.delete_selected", "删除所选");
        add("gui.ars_astra.manuscript.error.unknown_chart", "无法识别星图配置: %s");
        add("gui.ars_astra.manuscript.error.unknown_item", "缺少对物品的认知: %s");

        // 筛选器
        add("gui.ars_astra.filter.tab.icon", "图标");
        add("gui.ars_astra.filter.tab.item", "物品");
        add("gui.ars_astra.filter.tab.effect", "效果");
        add("gui.ars_astra.filter.elements_hint", "例如：生命, 物质...");
        add("gui.ars_astra.filter.tags_hint", "例如：logs, stones...");
        add("gui.ars_astra.filter.elements_label", "包含要素 (且)");
        add("gui.ars_astra.filter.tags_label", "包含标签 (且)");
        add("gui.ars_astra.filter.item_hint", "物品名称...");
        add("gui.ars_astra.filter.effect_hint", "效果名称...");
        add("gui.ars_astra.filter.item_label", "按物品筛选");
        add("gui.ars_astra.filter.effect_label", "按效果筛选");

        // 元素
        add(AAElements.ORDER, "秩序");
        add(AAElements.DEATH, "死亡");
        add(AAElements.MATTER, "物质");
        add(AAElements.LIFE, "生命");
        add(AAElements.STRUCTURE, "结构");
        add(AAElements.GROWTH, "生长");
        add(AAElements.CORROSION, "腐蚀");
        add(AAElements.DECAY, "凋零");

        // 分析台 GUI
        add("gui.ars_astra.analysis.btn_direct", "直接分析");
        add("gui.ars_astra.analysis.btn_intuition", "开始猜测");
        add("gui.ars_astra.analysis.btn_submit", "提交校准");
        add("gui.ars_astra.analysis.btn_batch", "一键分析");
        add("gui.ars_astra.analysis.btn_direct.tooltip", "直接消耗经验进行分析。");
        add("gui.ars_astra.analysis.btn_intuition.tooltip", "通过博弈小游戏进行分析。");
        add("gui.ars_astra.analysis.btn_batch.tooltip", "消耗经验一键分析背包中的所有物品。");
        add("gui.ars_astra.analysis.guesses_left", "剩余尝试: %s");
        add("gui.ars_astra.analysis.already_known", "该物品的性质已记录在案。");
        add("gui.ars_astra.analysis.not_enough_xp", "经验不足以进行直接解析。");
        add("gui.ars_astra.analysis.not_enough_xp_batch", "经验不足。分析所有物品共需 %s 经验。");
        add("gui.ars_astra.analysis.success", "解析完成，知识已记录。");
        add("gui.ars_astra.analysis.no_elements", "该物品似乎不含任何魔力要素。");
        add("gui.ars_astra.analysis.success_intuition", "完美的校准！你洞悉了它的本质。");
        add("gui.ars_astra.analysis.failure_destroyed", "操作失误导致样本损毁。");
        add("gui.ars_astra.analysis.guess_wrong", "校准失败。剩余机会: %s");
        add("gui.ars_astra.analysis.mode.precise", "精确校准（点击切换）");
        add("gui.ars_astra.analysis.mode.range", "模糊校准（点击切换）");
        add("gui.ars_astra.analysis.batch_success", "批量分析完成。成功记录了 %s 个新物品。");
        add("gui.ars_astra.analysis.batch_none", "没有找到可分析的新物品。");
    }

    private void add(Supplier<? extends Element> key, String name) {
        add(key.get().getDescriptionId(), name);
    }

}
