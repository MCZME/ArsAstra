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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    protected StarChart starChart;
    protected PlayerKnowledge knowledge;
    
    // 渲染状态
    private static final int STAR_COUNT = 300;
    private final List<BackgroundStar> backgroundStars = new ArrayList<>();
    private final long seed = 42L;
    
    // 几何缓存：存储经过细分和抖动处理后的手绘多边形顶点
    private final java.util.Map<Environment, List<Vector2f>> envGeometryCache = new java.util.WeakHashMap<>();
    
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

    private static class BackgroundStar {
        final float x, y, size, alpha;
        final int type; // 0: dot, 1: plus, 2: cross

        BackgroundStar(float x, float y, float size, float alpha, int type) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.alpha = alpha;
            this.type = type;
        }
    }

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, Component.empty());
        generateBackgroundStars();
    }

    private void generateBackgroundStars() {
        java.util.Random random = new java.util.Random(seed);
        for (int i = 0; i < STAR_COUNT; i++) {
            backgroundStars.add(new BackgroundStar(
                (random.nextFloat() - 0.5f) * 5000,
                (random.nextFloat() - 0.5f) * 5000,
                random.nextFloat() * 1.5f + 0.5f, // 0.5 - 2.0 size
                random.nextFloat() * 0.3f + 0.1f, // 0.1 - 0.4 alpha
                random.nextInt(3)
            ));
        }
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
                        circle.center().x + (float)Math.cos(angle) * circle.radius(),
                        circle.center().y + (float)Math.sin(angle) * circle.radius()
                    ));
                }
            } else if (shape instanceof Rectangle rect) {
                basePoints.add(new Vector2f(rect.min().x, rect.min().y));
                basePoints.add(new Vector2f(rect.max().x, rect.min().y));
                basePoints.add(new Vector2f(rect.max().x, rect.max().y));
                basePoints.add(new Vector2f(rect.min().x, rect.max().y));
            } else if (shape instanceof Polygon poly) {
                basePoints.addAll(poly.vertices());
            }
            
            return subdivideAndJitter(basePoints, 10.0f, 1.5f);
        });
    }

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
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        
        int ink = StarChartRenderUtils.Palette.INK;
        float r = (ink >> 16 & 255) / 255.0f;
        float g = (ink >> 8 & 255) / 255.0f;
        float b = (ink & 255) / 255.0f;

        for (BackgroundStar star : backgroundStars) {
            float alpha = star.alpha * Math.min(1.0f, currentScale * 2.0f);
            if (alpha < 0.05f) continue;

            float s = star.size / currentScale; 
            s = Math.max(0.5f, Math.min(3.0f, s));

            if (star.type == 0) { // Dot
                // For dots, we draw a tiny cross
                buffer.addVertex(matrix, star.x - s, star.y, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x + s, star.y, 0).setColor(r, g, b, alpha);
            } else if (star.type == 1) { // Plus
                buffer.addVertex(matrix, star.x - s, star.y, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x + s, star.y, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x, star.y - s, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x, star.y + s, 0).setColor(r, g, b, alpha);
            } else { // Cross
                buffer.addVertex(matrix, star.x - s, star.y - s, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x + s, star.y + s, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x + s, star.y - s, 0).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, star.x - s, star.y + s, 0).setColor(r, g, b, alpha);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void renderEnvironmentsDaVinci(GuiGraphics guiGraphics) {
        float lineWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        StarChartRenderUtils.LODLevel lod = StarChartRenderUtils.getLODLevel(scale);
        
        float blockAlpha = 0.0f;
        if (lod == StarChartRenderUtils.LODLevel.MACRO) {
            blockAlpha = 1.0f - Math.max(0.0f, (scale - 0.05f) / (0.2f - 0.05f));
        }

        for (Environment env : starChart.environments()) {
            List<Vector2f> handDrawn = getHandDrawnGeometry(env);
            
            int inkColor = (StarChartRenderUtils.Palette.INK & 0x00FFFFFF) | (180 << 24);
            float fadeWidth = 15.0f / scale; 
            fadeWidth = Math.max(10.0f, Math.min(40.0f, fadeWidth));
            
            StarChartRenderUtils.drawDaVinciHatchedPolygon(guiGraphics.pose(), handDrawn, 
                fadeWidth, inkColor, 0.5f, offsetX, offsetY);

            if (blockAlpha > 0.05f) {
                int fillColor = (StarChartRenderUtils.Palette.INK & 0x00FFFFFF) | ((int)(blockAlpha * 140) << 24);
                RenderSystem.setShaderColor(
                    ((fillColor >> 16) & 255) / 255f, 
                    ((fillColor >> 8) & 255) / 255f, 
                    (fillColor & 255) / 255f, 
                    blockAlpha * 0.6f);
                StarChartRenderUtils.drawSolidPolygon(guiGraphics.pose(), handDrawn);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            if (lod != StarChartRenderUtils.LODLevel.MACRO || scale > 0.15f) {
                StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), handDrawn, StarChartRenderUtils.Palette.INK, lineWidth);
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

            StarChartRenderUtils.drawSurveyCircle(guiGraphics.pose(), center, radius, StarChartRenderUtils.Palette.INK, lineWidth);

            if (hoveredField == field) {
                StarChartRenderUtils.drawDynamicCircle(guiGraphics.pose(), center, radius + 3 / scale, StarChartRenderUtils.Palette.CINNABAR, lineWidth * 0.8f);
            }

            if (this.scale > 0.2f) {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
                if (sprite != null) {
                    float iconSize = 24.0f;
                    StarChartRenderUtils.drawMonochromeIcon(guiGraphics.pose(), sprite, center, iconSize, StarChartRenderUtils.Palette.INK);
                }
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
