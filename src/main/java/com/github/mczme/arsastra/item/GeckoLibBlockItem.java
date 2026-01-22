package com.github.mczme.arsastra.item;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.renderer.GeckoLibBlockItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GeckoLibBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation modelPath;
    private final ResourceLocation texturePath;
    private final ResourceLocation animationPath;

    public GeckoLibBlockItem(Block block, Properties properties, String name) {
        super(block, properties);
        this.modelPath = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "geo/" + name + ".geo.json");
        this.texturePath = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/block/" + name + ".png");
        this.animationPath = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "animations/" + name + ".animation.json");
    }

    public ResourceLocation getModelPath() {
        return modelPath;
    }

    public ResourceLocation getTexturePath() {
        return texturePath;
    }

    public ResourceLocation getAnimationPath() {
        return animationPath;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeckoLibBlockItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GeckoLibBlockItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
