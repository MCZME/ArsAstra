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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    protected StarChart starChart;
    protected PlayerKnowledge knowledge;
    
    // 几何缓存：存储经过细分和抖动处理后的手绘多边形顶点
    private final Map<Environment, List<Vector2f>> envGeometryCache = new WeakHashMap<>();
    
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

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 2.0f;

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, Component.empty());
    }

    // --- 数据设置 ---

    public void setStarChart(StarChart starChart) {
        if (this.starChart != starChart) {
            this.starChart = starChart;
            this.envGeometryCache.clear(); // 切换星图时清空缓存
        }
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

    // --- 几何处理 ---
    private List<Vector2f> getHandDrawnGeometry(Environment env) {
        return envGeometryCache.computeIfAbsent(env, e -> {
            List<Vector2f> basePoints = new ArrayList<>();
            Shape shape = e.shape();

            if (shape instanceof Circle circle) {
                int segments = 32;
                for (int i = 0; i < segments; i++) {
                    float angle = (float) (i * 2 * Math.PI / segments);
                    basePoints.add(new Vector2f(
                            circle.center().x + (float) Math.cos(angle) * circle.radius(),
                            circle.center().y + (float) Math.sin(angle) * circle.radius()));
                }
            } else if (shape instanceof Rectangle rect) {
                basePoints.add(new Vector2f(rect.min().x, rect.min().y));
                basePoints.add(new Vector2f(rect.max().x, rect.min().y));
                basePoints.add(new Vector2f(rect.max().x, rect.max().y));
                basePoints.add(new Vector2f(rect.min().x, rect.max().y));
            } else if (shape instanceof Polygon poly) {
                basePoints.addAll(poly.vertices());
            } else if (shape instanceof com.github.mczme.arsastra.core.starchart.shape.ExteriorPolygon extPoly) {
                basePoints.addAll(extPoly.vertices());
            }

            return subdivideAndJitter(basePoints, 10.0f, 1.5f);
        });
    }

    // 细分和抖动多边形边缘以模拟手绘效果
    private List<Vector2f> subdivideAndJitter(List<Vector2f> points, float segmentLength, float jitterAmount) {
        if (points.isEmpty()) return points;
        java.util.Random rand = new java.util.Random(points.hashCode());
        List<Vector2f> result = new ArrayList<>();
        int count = points.size();
        for (int i = 0; i < count; i++) {
            Vector2f p1 = points.get(i);
            Vector2f p2 = points.get((i + 1) % count);
            result.add(new Vector2f(p1));
            float dist = p1.distance(p2);
            int segments = (int) Math.ceil(dist / segmentLength);
            for (int j = 1; j < segments; j++) {
                float t = (float) j / segments;
                Vector2f mid = new Vector2f(p1).lerp(p2, t);
                mid.add((rand.nextFloat() - 0.5f) * jitterAmount, (rand.nextFloat() - 0.5f) * jitterAmount);
                result.add(mid);
            }
        }
        return result;
    }

    // --- 核心渲染流程 ---
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateHoveredState(mouseX, mouseY);

        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        // L1: 底层 (纸张层)
        StarChartRenderUtils.drawParallaxLayer(guiGraphics.pose(), getX(), getY(), getWidth(), getHeight(),
            offsetX, offsetY, 0.1f, 512.0f, StarChartRenderUtils.PARCHMENT_TEXTURE, false);

        // L2: 中层 (背景装饰星)
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        float parallaxL2 = 0.4f;
        float centerX = getX() + getWidth() / 2.0f;
        float centerY = getY() + getHeight() / 2.0f;
        poseStack.translate(centerX, centerY, 0);
        poseStack.translate(offsetX * parallaxL2, offsetY * parallaxL2, 0);
        renderBackgroundStars(guiGraphics, scale);
        poseStack.popPose();

        // L3: 顶层 (核心星图层)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        if (starChart != null) {
            poseStack.pushPose();
            poseStack.translate(centerX, centerY, 0);
            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(scale, scale, 1.0f);

            renderEnvironmentsDaVinci(guiGraphics);
            renderEffectFields(guiGraphics);
            
            if (predictionPath != null && !predictionPath.isEmpty()) {
                renderPredictionPath(guiGraphics);
            }
            poseStack.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), StarChartRenderUtils.Palette.INK);
        guiGraphics.disableScissor();
    }

    // --- 分层渲染逻辑 ---
    private void renderBackgroundStars(GuiGraphics guiGraphics, float currentScale) {
        // TODO: 实现手绘风格的背景装饰星
    }

    private void renderEnvironmentsDaVinci(GuiGraphics guiGraphics) {
        float lineWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        
        for (Environment env : starChart.environments()) {
            List<Vector2f> handDrawn = getHandDrawnGeometry(env);
            
            // 使用淡墨色填充 (Alpha ~180)
            int inkColor = (StarChartRenderUtils.Palette.INK & 0x00FFFFFF) | (180 << 24);
            // UV Scale 设为 0.5，保持纹理在世界空间中的大小固定
            float textureDensity = 0.5f; 
            
            if (env.shape() instanceof com.github.mczme.arsastra.core.starchart.shape.ExteriorPolygon) {
                // ExteriorPolygon 使用墨水晕染效果
                StarChartRenderUtils.drawInkWashPolygonHollow(guiGraphics.pose(), handDrawn, 
                    inkColor, -offsetX, -offsetY);
            } else {
                // 普通多边形保留排线效果
                StarChartRenderUtils.drawHatchedPolygonFilled(guiGraphics.pose(), handDrawn, 
                    inkColor, textureDensity, -offsetX, -offsetY);
            }

            // 绘制手绘轮廓线 
            StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), handDrawn, StarChartRenderUtils.Palette.INK, lineWidth);
        }
    }

    private void renderEffectFields(GuiGraphics guiGraphics) {
        for (EffectField field : starChart.fields()) {
            net.minecraft.world.effect.MobEffect effect = field.getEffect();
            if (effect == null) continue;

            Vector2f center = field.center();
            float radius = field.getRadius();

            // 使用 Celestial Field Shader 渲染动态效果
            StarChartRenderUtils.drawCelestialField(guiGraphics.pose(), center, radius, effect.getColor());

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
            if (sprite != null) {
                float iconSize = 24.0f;
                StarChartRenderUtils.drawMonochromeIcon(guiGraphics.pose(), sprite, center, iconSize, StarChartRenderUtils.Palette.INK);
            }
        }
    }

    private void renderPredictionPath(GuiGraphics guiGraphics) {
        float baseWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        StarChartRenderUtils.drawPath(guiGraphics.pose(), predictionPath, baseWidth, 0xE6212A54);
        
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
            // 1. 获取缩放前的世界坐标
            Vector2f worldPosBefore = screenToWorld(mouseX, mouseY);
            
            // 2. 计算新缩放
            float zoomFactor = (scrollY > 0) ? 1.1f : 0.9f;
            float nextScale = this.scale * zoomFactor;
            nextScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, nextScale));
            
            // 3. 更新缩放
            this.scale = nextScale;
            
            // 4. 获取缩放后的新世界坐标
            Vector2f worldPosAfter = screenToWorld(mouseX, mouseY);
            
            // 5. 计算坐标差，并补偿到 offset，使得鼠标下的点保持不变
            this.offsetX += (worldPosAfter.x - worldPosBefore.x) * this.scale;
            this.offsetY += (worldPosAfter.y - worldPosBefore.y) * this.scale;

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

