package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.client.gui.util.Palette;
import com.github.mczme.arsastra.client.gui.util.PathRenderer;
import com.github.mczme.arsastra.client.gui.util.StarChartRenderUtils;
import com.github.mczme.arsastra.core.knowledge.PlayerKnowledge;
import com.github.mczme.arsastra.core.starchart.EffectField;
import com.github.mczme.arsastra.core.starchart.StarChart;
import com.github.mczme.arsastra.core.starchart.StarChartManager;
import com.github.mczme.arsastra.core.starchart.engine.DeductionResult;
import com.github.mczme.arsastra.core.starchart.engine.StarChartRoute;
import com.github.mczme.arsastra.core.starchart.environment.Environment;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import com.github.mczme.arsastra.core.starchart.shape.Circle;
import com.github.mczme.arsastra.core.starchart.shape.ExteriorPolygon;
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
import net.minecraft.world.effect.MobEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 星图核心渲染组件。
 * <p>
 * 负责渲染星图的所有可视元素，包括：
 * <ul>
 *     <li><b>环境背景 (L1):</b> 羊皮纸纹理背景。</li>
 *     <li><b>背景装饰 (L2):</b> 视差滚动的背景装饰星。</li>
 *     <li><b>星图主体 (L3):</b> 使用手绘风格 Shader 渲染的环境多边形。</li>
 *     <li><b>效果星域:</b> 动态光晕效果，显示已解锁的效果图标。</li>
 *     <li><b>推演路径:</b> 显示预测的炼金路径，支持幽灵路径预览。</li>
 * </ul>
 * 提供了完整的视口控制功能（平移、缩放）和坐标转换逻辑。
 */
@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    protected StarChart starChart;
    protected ResourceLocation starChartId;
    protected PlayerKnowledge knowledge;
    
    // 几何缓存：存储经过细分和抖动处理后的手绘多边形顶点
    private final Map<Environment, List<Vector2f>> envGeometryCache = new WeakHashMap<>();
    private final List<Vector2f> backgroundStars = new ArrayList<>();
    
    // 推演结果
    protected DeductionResult deductionResult;
    private int ghostStartIndex = -1;
    
    // 视口状态
    protected float scale = 0.1f;
    protected float offsetX = 0;
    protected float offsetY = 0;
    protected boolean isDragging = false;

    // 交互状态
    private EffectField hoveredField = null;

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 2.0f;

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, Component.empty());
        initBackgroundStars();
    }
    
    private void initBackgroundStars() {
        Random rand = new Random(12345); // 固定种子以保持一致性
        for (int i = 0; i < 50; i++) {
            float x = (rand.nextFloat() - 0.5f) * 2000.0f;
            float y = (rand.nextFloat() - 0.5f) * 2000.0f;
            backgroundStars.add(new Vector2f(x, y));
        }
    }

    // --- 数据设置 ---

    public void setStarChart(ResourceLocation id, StarChart starChart) {
        this.starChartId = id;
        if (this.starChart != starChart) {
            this.starChart = starChart;
            this.envGeometryCache.clear(); // 切换星图时清空缓存
            // 可以根据星图种子重新生成背景星，这里暂且保持一致
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
                        .ifPresent(chart -> this.setStarChart(firstId, chart));
            }
        }
    }

    public void setDeductionResult(DeductionResult result) {
        this.deductionResult = result;
    }

    /**
     * 设置幽灵路径的起始段索引。
     * @param index 起始索引，-1 表示没有幽灵路径。
     */
    public void setGhostStartIndex(int index) {
        this.ghostStartIndex = index;
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
            } else if (shape instanceof ExteriorPolygon extPoly) {
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

            renderEnvironments(guiGraphics);
            renderEffectFields(guiGraphics);
            
            if (deductionResult != null) {
                renderPredictionPath(guiGraphics);
            }
            poseStack.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), Palette.INK);
        guiGraphics.disableScissor();
    }

    // --- 分层渲染逻辑 ---
    /**
     * 渲染背景装饰星。简单的点渲染。
     */
    private void renderBackgroundStars(GuiGraphics guiGraphics, float currentScale) {
        
    }

    private void renderEnvironments(GuiGraphics guiGraphics) {
        float lineWidth = StarChartRenderUtils.getScaleCompensatedWidth(2.5f, scale);
        
        for (Environment env : starChart.environments()) {
            List<Vector2f> handDrawn = getHandDrawnGeometry(env);
            
            // 使用淡墨色填充 (Alpha ~180)
            int inkColor = (Palette.INK & 0x00FFFFFF) | (180 << 24);
            // UV Scale 设为 0.5，保持纹理在世界空间中的大小固定
            float textureDensity = 0.5f; 
            
            if (env.shape() instanceof ExteriorPolygon) {
                // ExteriorPolygon 使用墨水晕染效果
                StarChartRenderUtils.drawInkWashPolygonHollow(guiGraphics.pose(), handDrawn, 
                    inkColor, -offsetX, -offsetY);
            } else {
                // 普通多边形保留排线效果
                StarChartRenderUtils.drawHatchedPolygonFilled(guiGraphics.pose(), handDrawn, 
                    inkColor, textureDensity, -offsetX, -offsetY);
            }

            // 绘制手绘轮廓线 
            StarChartRenderUtils.drawDynamicLoop(guiGraphics.pose(), handDrawn, Palette.INK, lineWidth);
        }
    }

    /**
     * 渲染效果星域（光环与图标）。支持根据玩家知识显示/隐藏。
     */
    private void renderEffectFields(GuiGraphics guiGraphics) {
        List<EffectField> fields = starChart.fields();
        for (int i = 0; i < fields.size(); i++) {
            EffectField field = fields.get(i);
            MobEffect effect = field.getEffect();
            if (effect == null) continue;

            Vector2f center = field.center();
            float radius = field.getRadius();
            
            boolean unlocked = knowledge != null && starChartId != null && knowledge.hasUnlockedField(starChartId, i);

            // 1. 渲染光环 (Celestial Field Shader)
            // 未解锁时使用墨色，解锁后使用效果色
            int fieldColor = unlocked ? effect.getColor() : Palette.INK;
            StarChartRenderUtils.drawCelestialField(guiGraphics.pose(), center, radius, fieldColor);

            // 2. 渲染图标或问号
            if (unlocked) {
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
                if (sprite != null) {
                    float iconSize = 24.0f;
                    StarChartRenderUtils.drawMonochromeIcon(guiGraphics.pose(), sprite, center, iconSize, Palette.INK);
                }
            } else {
                // 绘制 "???"
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(center.x, center.y, 0);
                // 保持文字大小不随缩放变化太大，但要适应当前视图
                // 这里我们希望它像地图上的标记一样，所以稍微反向缩放一点？或者就固定大小
                float textScale = 1.0f / Math.max(0.5f, scale); // 简单的反向缩放
                pose.scale(textScale, textScale, 1.0f);
                
                String text = "???";
                int color = Palette.INK;
                int textWidth = Minecraft.getInstance().font.width(text);
                guiGraphics.drawString(Minecraft.getInstance().font, text, -textWidth / 2, -4, color, false);
                
                pose.popPose();
            }
        }
    }

    private void renderPredictionPath(GuiGraphics guiGraphics) {
        if (deductionResult == null) return;
        
        float baseWidth = StarChartRenderUtils.getScaleCompensatedWidth(4.0f, scale);
        StarChartRoute route = deductionResult.route();
        
        if (route.segments().isEmpty()) return;

        List<Vector2f> confirmedPoints = new ArrayList<>();
        List<Vector2f> ghostPoints = new ArrayList<>();
        
        List<StarChartPath> segments = route.segments();
        for (int i = 0; i < segments.size(); i++) {
            // 使用统一采样接口，步长设为 2.0
            List<Vector2f> points = segments.get(i).sample(2.0f);
            
            if (ghostStartIndex >= 0 && i >= ghostStartIndex) {
                // 幽灵段
                ghostPoints.addAll(points);
            } else {
                // 确认段
                confirmedPoints.addAll(points);
            }
        }
        
        // 渲染确认路径 (深色实线)
        if (!confirmedPoints.isEmpty()) {
            PathRenderer.renderPencilPath(guiGraphics.pose(), confirmedPoints, baseWidth, Palette.INK, 4.0f);
        }

        // 渲染幽灵路径 (半透明)
        if (!ghostPoints.isEmpty()) {
            // 降低 Alpha 值模拟半透明
            int ghostColor = (Palette.INK & 0x00FFFFFF) | (128 << 24);
            PathRenderer.renderPencilPath(guiGraphics.pose(), ghostPoints, baseWidth, ghostColor, 4.0f);
        }
        
        // 绘制末端光标
        List<Vector2f> activePoints = ghostPoints.isEmpty() ? confirmedPoints : ghostPoints;
        if (!activePoints.isEmpty()) {
            Vector2f lastPoint = activePoints.get(activePoints.size() - 1);
            renderDraftingCursor(guiGraphics, lastPoint);
        }
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
            this.offsetX += (float) dragX;
            this.offsetY += (float) dragY;
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

    public void centerOn(Vector2f pos) {
        this.offsetX = -pos.x * scale;
        this.offsetY = -pos.y * scale;
    }

    public Vector2f screenToWorld(double mouseX, double mouseY) {
        float centerX = getX() + getWidth() / 2.0f;
        float centerY = getY() + getHeight() / 2.0f;
        return new Vector2f(
            (float) (mouseX - centerX - offsetX) / scale,
            (float) (mouseY - centerY - offsetY) / scale
        );
    }
    
    public EffectField getHoveredField() {
        return hoveredField;
    }
    
    public boolean isHoveredFieldUnlocked() {
        if (hoveredField == null || starChart == null || knowledge == null || starChartId == null) return false;
        int index = starChart.fields().indexOf(hoveredField);
        return index >= 0 && knowledge.hasUnlockedField(starChartId, index);
    }

    public void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredField != null) {
            List<Component> tooltips = new ArrayList<>();
            if (isHoveredFieldUnlocked()) {
                MobEffect effect = hoveredField.getEffect();
                if (effect != null) {
                    tooltips.add(effect.getDisplayName());
                    // 暂时只显示名称，后续可以显示更多信息如半径
                }
            } else {
                tooltips.add(Component.translatable("gui.ars_astra.atlas.field.unknown").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            if (!tooltips.isEmpty()) {
                guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, tooltips, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
