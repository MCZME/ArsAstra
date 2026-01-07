package com.github.mczme.arsastra.client;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.gui.StarChartJournalScreen;
import com.github.mczme.arsastra.registry.AAMenus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = ArsAstra.MODID, value = Dist.CLIENT)
public class AAClientEvents {

    private static ShaderInstance monochromeShader;
    private static ShaderInstance davinciHatchingShader;

    public static ShaderInstance getMonochromeShader() {
        return monochromeShader;
    }

    public static ShaderInstance getDavinciHatchingShader() {
        return davinciHatchingShader;
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AAMenus.STAR_CHART_JOURNAL_MENU.get(), StarChartJournalScreen::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "monochrome"), DefaultVertexFormat.POSITION_TEX_COLOR),
                shaderInstance -> {
                    monochromeShader = shaderInstance;
                });

        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "davinci_hatching"), DefaultVertexFormat.POSITION_TEX_COLOR),
                shaderInstance -> {
                    davinciHatchingShader = shaderInstance;
                });
    }
}
