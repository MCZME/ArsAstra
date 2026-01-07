package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.shape.Circle;
import com.github.mczme.arsastra.core.starchart.shape.Polygon;
import com.github.mczme.arsastra.core.starchart.shape.Rectangle;
import com.github.mczme.arsastra.core.starchart.shape.Shape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    protected StarChart starChart;
    protected PlayerKnowledge knowledge;
    
    // 渲染状态
    private static final int STAR_COUNT = 200;
    private final List<Vector2f> backgroundStars = new ArrayList<>();
    private final long seed = 42L;
    
    // 推演路径
    protected List<Vector2f> predictionPath;
    protected float predictedStability;
    
    // 视口状态
    protected float scale = 0.1f;
    protected float offsetX = 0;
    protected float offsetY = 0;
    protected boolean isDragging = false;
    protected double lastMouseX, lastMouseY;

    // 交互状态
    private EffectField hoveredField = null;

    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 5.0f;

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, Component.empty()); // AbstractWidget requires a Message
        generateBackgroundStars();
    }

    private void generateBackgroundStars() {
        java.util.Random random = new java.util.Random(seed);
        for (int i = 0; i < STAR_COUNT; i++) {
            backgroundStars.add(new Vector2f(
                (random.nextFloat() - 0.5f) * 4000,
                (random.nextFloat() - 0.5f) * 4000
            ));
        }
    }

    // --- 数据设置 ---

    public void setStarChart(StarChart starChart) {
        this.starChart = starChart;
    }

    public void setKnowledge(PlayerKnowledge knowledge) {
        this.knowledge = knowledge;
        if (this.starChart == null && knowledge != null) {
            java.util.Set<ResourceLocation> visited = knowledge.getVisitedStarCharts();
            if (!visited.isEmpty()) {
                ResourceLocation firstId = visited.iterator().next();
                StarChartManager.getInstance()
                        .getStarChart(firstId)
                        .ifPresent(this::setStarChart);
            }
        }
    }

    public void setPrediction(List<Vector2f> path, float stability) {
        this.predictionPath = path;
        this.predictedStability = stability;
    }

    // --- 核心渲染流程 ---

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateHoveredState(mouseX, mouseY);

        // 1. 设置裁剪
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        // --- L1: 底层 (纸张层) ---
        // 视差系数 0.1，模拟最远的背景
        StarChartRenderUtils.drawParallaxLayer(guiGraphics.pose(), getX(), getY(), getWidth(), getHeight(),
            offsetX, offsetY, 0.1f, 512.0f, StarChartRenderUtils.PARCHMENT_TEXTURE, false);

        // --- L2: 中层 (背景装饰星) ---
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        // L2 视差变换 (系数 0.4)
        float parallaxL2 = 0.4f;
        float centerX = getX() + getWidth() / 2.0f;
        float centerY = getY() + getHeight() / 2.0f;
        
        poseStack.translate(centerX, centerY, 0);
        poseStack.translate(offsetX * parallaxL2, offsetY * parallaxL2, 0);
        
        renderBackgroundStars(guiGraphics, scale);
        poseStack.popPose();

        // --- L3: 顶层 (核心星图层) ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // 3a. Stencil Mask Pass
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.translate(offsetX, offsetY, 0);
        poseStack.scale(scale, scale, 1.0f);

        if (starChart != null) {
            StarChartRenderUtils.beginStencilMask();
            renderEnvironmentMasks(guiGraphics);
            StarChartRenderUtils.applyStencilMask();

            // 3b. Global Hatching Pass
            poseStack.popPose(); // 暂时回到屏幕空间
            
            // 计算 LOD 和排线透明度
            StarChartRenderUtils.LODLevel lod = StarChartRenderUtils.getLODLevel(scale);
            int hatchingColor = StarChartRenderUtils.Palette.INK;
            
            float hatchingScale;
            
            if (scale < 0.1f) {
                hatchingScale = 1.0f - scale;
            } else if (scale < 1.0f) {
                hatchingScale = scale * 0.2f + 0.88f;
            } else {
                hatchingScale = 1.08f - scale * 0.3f;
            }

            if ((hatchingColor >> 24 & 255) > 5) {
                StarChartRenderUtils.drawGlobalHatching(guiGraphics.pose(), getX(), getY(), getWidth(), getHeight(), 
                    hatchingScale, offsetX, offsetY, hatchingColor);
            }
            
            StarChartRenderUtils.endStencilMask();
            
            // 重新应用 L3 变换
            poseStack.pushPose();
            poseStack.translate(centerX, centerY, 0);
            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(scale, scale, 1.0f);

            // 3c. Outlines & Icons Pass
            renderEnvironments(guiGraphics);
            renderEffectFields(guiGraphics);
        }

        if (predictionPath != null && !predictionPath.isEmpty()) {
            renderPredictionPath(guiGraphics);
        }

        poseStack.popPose();
        
        // 5. 恢复状态和绘制边框
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), StarChartRenderUtils.Palette.INK);
        guiGraphics.disableScissor();
    }

    // --- 分层渲染逻辑 ---

    private void renderEnvironmentMasks(GuiGraphics guiGraphics) {
        for (Environment env : starChart.environments()) {
            Shape shape = env.shape();
            if (shape instanceof Circle circle) {
                StarChartRenderUtils.drawSolidCircle(guiGraphics.pose(), circle.center(), circle.radius());
            } else if (shape instanceof Rectangle rect) {
                List<Vector2f> poly = Arrays.asList(
                    new Vector2f(rect.min().x, rect.min().y), new Vector2f(rect.max().x, rect.min().y),
                    new Vector2f(rect.max().x, rect.max().y), new Vector2f(rect.min().x, rect.max().y)
                );
                StarChartRenderUtils.drawSolidPolygon(guiGraphics.pose(), poly);
            } else if (shape instanceof Polygon poly) {
                StarChartRenderUtils.drawSolidPolygon(guiGraphics.pose(), poly.vertices());
            }
        }
    }

    private void renderBackgroundStars(GuiGraphics guiGraphics, float currentScale) {
        // TODO: 重新实现符合达芬奇风格的背景星渲染
    }

    private void renderEnvironments(GuiGraphics guiGraphics) {
        float lineWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        StarChartRenderUtils.LODLevel lod = StarChartRenderUtils.getLODLevel(scale);
        
        // 宏观视角：墨块显现
        float blockAlpha = 0.0f;
        if (lod == StarChartRenderUtils.LODLevel.MACRO) {
            blockAlpha = 1.0f - Math.max(0.0f, (scale - 0.05f) / (0.2f - 0.05f));
        }

        for (Environment env : starChart.environments()) {
            Shape shape = env.shape();
            String typeId = env.getType().getDescriptionId();
            
            // 根据环境类型选择古典颜色
            int tintColor = StarChartRenderUtils.Palette.INDIGO; // 默认：靛青 (秩序/奥法)
            if (typeId.contains("chaos") || typeId.contains("crimson")) {
                tintColor = StarChartRenderUtils.Palette.CINNABAR; // 朱砂 (混沌/血腥)
            } else if (typeId.contains("nature") || typeId.contains("growth")) {
                tintColor = StarChartRenderUtils.Palette.MALACHITE; // 孔雀石绿 (自然)
            }

            // 2. 绘制实心墨块 (LOD: Macro)
            if (blockAlpha > 0.05f) {
                int fillColor = (StarChartRenderUtils.Palette.INK & 0x00FFFFFF) | ((int)(blockAlpha * 180) << 24);
                RenderSystem.setShaderColor(1f, 1f, 1f, blockAlpha * 0.7f);
                // (此处省略重复的绘制逻辑，由于 blockAlpha 主要用于极远距离，我们直接复用上面的 shapeDrawer 思路)
                RenderSystem.setShaderColor(
                    ((fillColor >> 16) & 255) / 255f, 
                    ((fillColor >> 8) & 255) / 255f, 
                    (fillColor & 255) / 255f, 
                    blockAlpha * 0.7f);
                
                if (shape instanceof Circle circle) StarChartRenderUtils.drawSolidCircle(guiGraphics.pose(), circle.center(), circle.radius());
                else if (shape instanceof Rectangle rect) {
                    List<Vector2f> poly = Arrays.asList(new Vector2f(rect.min().x, rect.min().y), new Vector2f(rect.max().x, rect.min().y), new Vector2f(rect.max().x, rect.max().y), new Vector2f(rect.min().x, rect.max().y));
                    StarChartRenderUtils.drawSolidPolygon(guiGraphics.pose(), poly);
                }
                else if (shape instanceof Polygon poly) StarChartRenderUtils.drawSolidPolygon(guiGraphics.pose(), poly.vertices());
                
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            // 3. 绘制轮廓线 (LOD: Normal/Detail)
            if (lod != StarChartRenderUtils.LODLevel.MACRO || scale > 0.15f) {
                int outlineColor = StarChartRenderUtils.Palette.INK;
                if (shape instanceof Circle circle) {
                    StarChartRenderUtils.drawDynamicCircle(guiGraphics.pose(), circle.center(), circle.radius(), outlineColor, lineWidth);
                } else if (shape instanceof Rectangle rect) {
                    List<Vector2f> poly = Arrays.asList(
                        new Vector2f(rect.min().x, rect.min().y), new Vector2f(rect.max().x, rect.min().y),
                        new Vector2f(rect.max().x, rect.max().y), new Vector2f(rect.min().x, rect.max().y)
                    );
                    StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), poly, outlineColor, lineWidth);
                } else if (shape instanceof Polygon poly) {
                    StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), poly.vertices(), outlineColor, lineWidth);
                }
            }
        }
    }

    private void renderEffectFields(GuiGraphics guiGraphics) {
        float lineWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.0f, scale);
        
        for (EffectField field : starChart.fields()) {
            net.minecraft.world.effect.MobEffect effect = field.getEffect();
            if (effect == null) continue;

            Vector2f center = field.center();
            float radius = field.getRadius();

            // 1. 绘制达芬奇风格测绘圆 (主墨色)
            StarChartRenderUtils.drawSurveyCircle(guiGraphics.pose(), center, radius, StarChartRenderUtils.Palette.INK, lineWidth);

            // 2. 悬停反馈：增加一圈朱砂色的强调草稿环
            if (hoveredField == field) {
                StarChartRenderUtils.drawDynamicCircle(guiGraphics.pose(), center, radius + 3 / scale, StarChartRenderUtils.Palette.CINNABAR, lineWidth * 0.8f);
            }

            // 3. 绘制图标 (仅在非微缩视角显示)
            if (this.scale > 0.2f) {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
                if (sprite != null) {
                    float iconSize = 24.0f; // 保持世界单位大小
                    StarChartRenderUtils.drawMonochromeIcon(guiGraphics.pose(), sprite, center, iconSize, StarChartRenderUtils.Palette.INK);
                }
            }
        }
    }

    private void renderPredictionPath(GuiGraphics guiGraphics) {
        float baseWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        
        // 绘制路径
        StarChartRenderUtils.drawPath(guiGraphics.pose(), predictionPath, baseWidth, 0xE6212A54);
        
        // 绘制游标
        Vector2f lastPoint = predictionPath.get(predictionPath.size() - 1);
        renderDraftingCursor(guiGraphics, lastPoint);
    }

    private void renderDraftingCursor(GuiGraphics guiGraphics, Vector2f pos) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, 0);
        float cursorSize = 4.0f / scale;
        cursorSize = Math.max(2.0f, Math.min(8.0f, cursorSize));

        int color = 0xCC212A54;
        guiGraphics.fill(-(int)cursorSize, 0, (int)cursorSize, 1, color);
        guiGraphics.fill(0, -(int)cursorSize, 1, (int)cursorSize, color);
        poseStack.popPose();
    }

    // --- 交互与工具 ---

    private void updateHoveredState(int mouseX, int mouseY) {
        hoveredField = null;
        if (starChart == null || !isMouseOver(mouseX, mouseY)) return;

        Vector2f worldPos = screenToWorld(mouseX, mouseY);
        for (EffectField field : starChart.fields()) {
            if (worldPos.distance(field.center()) <= field.getRadius()) {
                hoveredField = field;
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && button == 0) {
            this.isDragging = true;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            double dx = mouseX - this.lastMouseX;
            double dy = mouseY - this.lastMouseY;
            this.offsetX += (float) dx;
            this.offsetY += (float) dy;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            float zoomFactor = (scrollY > 0) ? 1.1f : 0.9f;
            float nextScale = this.scale * zoomFactor;
            if (nextScale >= MIN_SCALE && nextScale <= MAX_SCALE) {
                this.scale = nextScale;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public Vector2f screenToWorld(double mouseX, double mouseY) {
        float centerX = getX() + getWidth() / 2.0f;
        float centerY = getY() + getHeight() / 2.0f;
        return new Vector2f(
            (float) (mouseX - centerX - offsetX) / scale,
            (float) (mouseY - centerY - offsetY) / scale
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}