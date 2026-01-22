package com.github.mczme.arsastra.client.model;

import com.github.mczme.arsastra.item.GeckoLibBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GeckoLibBlockItemModel extends GeoModel<GeckoLibBlockItem> {
    @Override
    public ResourceLocation getModelResource(GeckoLibBlockItem animatable) {
        return animatable.getModelPath();
    }

    @Override
    public ResourceLocation getTextureResource(GeckoLibBlockItem animatable) {
        return animatable.getTexturePath();
    }

    @Override
    public ResourceLocation getAnimationResource(GeckoLibBlockItem animatable) {
        return animatable.getAnimationPath();
    }
}
