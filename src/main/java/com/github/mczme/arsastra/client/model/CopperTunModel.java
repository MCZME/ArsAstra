package com.github.mczme.arsastra.client.model;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CopperTunModel extends GeoModel<CopperTunBlockEntity> {
    @Override
    public ResourceLocation getModelResource(CopperTunBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "geo/copper_tun.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CopperTunBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/block/copper_tun.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CopperTunBlockEntity object) {
        return ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "animations/copper_tun.animation.json");
    }
}
