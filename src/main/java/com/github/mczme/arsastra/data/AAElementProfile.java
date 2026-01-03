package com.github.mczme.arsastra.data;

import com.github.mczme.arsastra.data.provider.ElementProfileProvider;
import com.github.mczme.arsastra.registry.AAElements;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.joml.Vector2f;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AAElementProfile extends ElementProfileProvider {

    public AAElementProfile(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, existingFileHelper);
    }

    @Override
    protected void gather() {
        basic("vanilla_ingredients", 100, basicBuilder()
                .add(Items.REDSTONE, new Vector2f(0.0f, 300.0f), Map.of(
                        AAElements.ORDER, 18.0f,
                        AAElements.STRUCTURE, 6.0f
                ))
                .add(Items.SUGAR, new Vector2f(-158.0f, 618.0f), Map.of(
                        AAElements.ORDER, 10.0f,
                        AAElements.GROWTH, 5.0f
                ))
                .add(Items.RABBIT_FOOT, new Vector2f(-58.0f, 923.0f), Map.of(
                        AAElements.ORDER, 30.0f,
                        AAElements.LIFE, 10.0f
                ))
                .add(Items.BLAZE_POWDER, new Vector2f(-488.0f, 769.0f), Map.of(
                        AAElements.ORDER, 15.0f,
                        AAElements.CORROSION, 10.0f
                ))
                .add(Items.GHAST_TEAR, new Vector2f(639.0f, 321.0f), Map.of(
                        AAElements.LIFE, 35.0f,
                        AAElements.ORDER, 15.0f
                ))
                .add(Items.GLISTERING_MELON_SLICE, new Vector2f(813.0f, 493.0f), Map.of(
                        AAElements.LIFE, 20.0f,
                        AAElements.ORDER, 5.0f
                ))
                .add(Items.PUFFERFISH, new Vector2f(484.0f, 566.0f), Map.of(
                        AAElements.LIFE, 12.0f,
                        AAElements.MATTER, 8.0f
                ))
                .add(Items.MAGMA_CREAM, new Vector2f(603.0f, 701.0f), Map.of(
                        AAElements.MATTER, 15.0f,
                        AAElements.ORDER, 10.0f
                ))
                .add(Items.PHANTOM_MEMBRANE, new Vector2f(-621.0f, 393.0f), Map.of(
                        AAElements.ORDER, 18.0f,
                        AAElements.DEATH, 7.0f
                ))
                .add(Items.GOLDEN_CARROT, new Vector2f(-719.0f, 250.0f), Map.of(
                        AAElements.ORDER, 20.0f,
                        AAElements.LIFE, 10.0f
                ))
                .add(Items.SPIDER_EYE, new Vector2f(468.0f, -537.0f), Map.of(
                        AAElements.DECAY, 12.0f,
                        AAElements.LIFE, 3.0f
                ))
                .add(Items.BREEZE_ROD, new Vector2f(-313.0f, -849.0f), Map.of(
                        AAElements.ORDER, 35.0f,
                        AAElements.STRUCTURE, 10.0f
                ))
                .add(Items.COBWEB, new Vector2f(-214.0f, -533.0f), Map.of(
                        AAElements.STRUCTURE, 15.0f,
                        AAElements.DECAY, 5.0f
                ))
                .add(Items.SLIME_BLOCK, new Vector2f(-283.0f, -706.0f), Map.of(
                        AAElements.MATTER, 30.0f,
                        AAElements.LIFE, 10.0f
                ))
                .add(Items.STONE, new Vector2f(214.0f, -533.0f), Map.of(
                        AAElements.MATTER, 5.0f,
                        AAElements.STRUCTURE, 5.0f
                ))
        );
    }
}
