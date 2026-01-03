package com.github.mczme.arsastra.client.model;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.item.StarChartJournalItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StarChartJournalModel extends GeoModel<StarChartJournalItem> {
    @Override
    public ResourceLocation getModelResource(StarChartJournalItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "geo/star_chart_journal.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StarChartJournalItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/item/star_chart_journal.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StarChartJournalItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "animations/star_chart_journal.animation.json");
    }
}
