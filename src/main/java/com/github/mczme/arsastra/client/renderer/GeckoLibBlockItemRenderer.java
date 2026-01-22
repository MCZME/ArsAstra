package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.client.model.GeckoLibBlockItemModel;
import com.github.mczme.arsastra.item.GeckoLibBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GeckoLibBlockItemRenderer extends GeoItemRenderer<GeckoLibBlockItem> {
    public GeckoLibBlockItemRenderer() {
        super(new GeckoLibBlockItemModel());
    }
}
