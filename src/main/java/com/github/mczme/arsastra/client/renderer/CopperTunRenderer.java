package com.github.mczme.arsastra.client.renderer;

import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.github.mczme.arsastra.client.model.CopperTunModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CopperTunRenderer extends GeoBlockRenderer<CopperTunBlockEntity> {
    public CopperTunRenderer(BlockEntityRendererProvider.Context context) {
        super(new CopperTunModel());
    }
}