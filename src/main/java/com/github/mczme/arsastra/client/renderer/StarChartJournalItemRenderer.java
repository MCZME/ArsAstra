package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.client.model.StarChartJournalModel;
import com.github.mczme.arsastra.item.StarChartJournalItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class StarChartJournalItemRenderer extends GeoItemRenderer<StarChartJournalItem> {
    public StarChartJournalItemRenderer() {
        super(new StarChartJournalModel());
    }
}
