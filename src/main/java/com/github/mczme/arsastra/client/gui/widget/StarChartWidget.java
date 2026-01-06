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

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StarChartWidget extends AbstractWidget {
    protected StarChart starChart;
    protected PlayerKnowledge knowledge;
    
    // 推演路径数据
    protected List<Vector2f> predictionPath;
    protected float predictedStability;
    
    // 视口状态
    protected float scale = 0.1f;
    protected float offsetX = 0;
    protected float offsetY = 0;
    protected boolean isDragging = false;
    protected double lastMouseX, lastMouseY;

    // 缩放限制
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 5.0f;

    public StarChartWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setStarChart(StarChart starChart) {
        this.starChart = starChart;
    }

    public void setKnowledge(PlayerKnowledge knowledge) {
        this.knowledge = knowledge;
        // 如果当前没有设置星图，尝试从玩家知识库中加载第一个
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

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 剪裁测试，防止绘制到组件外
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        
        // 2. 绘制深色背景
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF08080A);

        // 开启混合并设置正确的混合函数，这是透明显示的关键
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 3. 应用视口变换
        poseStack.translate(getX() + getWidth() / 2.0f, getY() + getHeight() / 2.0f, 0);
        poseStack.translate(offsetX, offsetY, 0);
        poseStack.scale(scale, scale, 1.0f);

        // 4. 绘制参考轴
        renderAxes(guiGraphics);

        // 5. 绘制星图环境
        if (starChart != null) {
            renderStarChart(guiGraphics);
        }

        // 6. 绘制推演路径
        if (predictionPath != null && !predictionPath.isEmpty()) {
            renderPredictionPath(guiGraphics);
        }

        poseStack.popPose();
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        // 7. 绘制边框
        guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF4A3B30);

        guiGraphics.disableScissor();
    }

    private void renderAxes(GuiGraphics guiGraphics) {
        int axisColor = 0x22FFFFFF;
        int length = 2000;
        // 绘制十字参考线
        guiGraphics.fill(-length, 0, length, 1, axisColor);
        guiGraphics.fill(0, -length, 1, length, axisColor);
    }

    private void renderStarChart(GuiGraphics guiGraphics) {
        // 1. 渲染环境
        for (Environment env : starChart.environments()) {
            renderEnvironment(guiGraphics, env);
        }
        
        // 2. 渲染效果星域
        for (EffectField field : starChart.fields()) {
            renderEffectField(guiGraphics, field);
        }
    }

    private void renderEffectField(GuiGraphics guiGraphics, EffectField field) {
        net.minecraft.world.effect.MobEffect effect = field.getEffect();
        if (effect == null) return;

        int color = effect.getColor();
        float radius = field.getRadius();
        Vector2f center = field.center();

        // 1. 绘制效果范围圆 (半透明实心)
        // Alpha 设为 0.3 (0x4C) 增加点辨识度
        int fieldColor = (0x4C << 24) | (color & 0x00FFFFFF);
        drawCircle(guiGraphics, center, radius, fieldColor);

        // 2. 绘制效果图标 (仅在缩放倍率足够大时显示)
        if (this.scale > 0.2f) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getMobEffectTextures().get(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
            
            if (sprite != null) {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(center.x, center.y, 0);
                // 增大图标大小至 24x24
                float iconSize = 24.0f;
                // 设置图标渲染颜色为纯白
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                guiGraphics.blit((int)(-iconSize/2), (int)(-iconSize/2), 0, (int)iconSize, (int)iconSize, sprite);
                poseStack.popPose();
            }
        }
    }

    private void renderEnvironment(GuiGraphics guiGraphics, Environment env) {
        Shape shape = env.shape();
        // 基础颜色方案
        int color = 0x445588FF; 
        
        String typeId = env.getType().getDescriptionId();
        if (typeId.contains("chaos")) color = 0x44FF55FF;
        else if (typeId.contains("entropy")) color = 0x4455FF55;

        if (shape instanceof Circle circle) {
            drawCircle(guiGraphics, circle.center(), circle.radius(), color);
        } else if (shape instanceof Rectangle rect) {
            drawRectangle(guiGraphics, rect.min(), rect.max(), color);
        } else if (shape instanceof Polygon poly) {
            drawPolygon(guiGraphics, poly.vertices(), color);
        }
    }

    private void renderPredictionPath(GuiGraphics guiGraphics) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = guiGraphics.pose().last().pose();

        for (Vector2f p : predictionPath) {
            buffer.addVertex(matrix, p.x, p.y, 0).setColor(1.0f, 1.0f, 1.0f, 0.8f);
        }
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    // 几何绘制辅助方法
    
    private void drawCircle(GuiGraphics guiGraphics, Vector2f center, float radius, int color) {
        int segments = 32;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float a = (color >> 24 & 255) / 255.0f;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        buffer.addVertex(matrix, center.x, center.y, 0).setColor(r, g, b, a);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            buffer.addVertex(matrix, center.x + (float)Math.cos(angle) * radius, center.y + (float)Math.sin(angle) * radius, 0)
                  .setColor(r, g, b, a);
        }
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        // 绘制边缘描边
        BufferBuilder lineBuffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            lineBuffer.addVertex(matrix, center.x + (float)Math.cos(angle) * radius, center.y + (float)Math.sin(angle) * radius, 0)
                      .setColor(r, g, b, Math.min(1.0f, a * 2.0f));
        }
        BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
    }

    private void drawRectangle(GuiGraphics guiGraphics, Vector2f min, Vector2f max, int color) {
        guiGraphics.fill((int)min.x, (int)min.y, (int)max.x, (int)max.y, color);
        // 绘制边框
        guiGraphics.renderOutline((int)min.x, (int)min.y, (int)(max.x - min.x), (int)(max.y - min.y), color | 0xFF000000);
    }

    private void drawPolygon(GuiGraphics guiGraphics, List<Vector2f> vertices, int color) {
        if (vertices.size() < 3) return;
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float a = (color >> 24 & 255) / 255.0f;

        for (Vector2f v : vertices) {
            buffer.addVertex(matrix, v.x, v.y, 0).setColor(r, g, b, a);
        }
        
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        // 描边
        BufferBuilder lineBuffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (Vector2f v : vertices) {
            lineBuffer.addVertex(matrix, v.x, v.y, 0).setColor(r, g, b, 0.8f);
        }
        // 回到起点
        lineBuffer.addVertex(matrix, vertices.get(0).x, vertices.get(0).y, 0).setColor(r, g, b, 0.8f);
        BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    // 交互逻辑
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
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            // 手动计算 delta，不依赖 dragX/dragY 参数
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

    /**
     * 将屏幕坐标转换为世界坐标 (逻辑坐标)
     */
    public Vector2f screenToWorld(double mouseX, double mouseY) {
        float centerX = getX() + getWidth() / 2.0f;
        float centerY = getY() + getHeight() / 2.0f;
        float relX = (float) (mouseX - centerX - offsetX) / scale;
        float relY = (float) (mouseY - centerY - offsetY) / scale;
        return new Vector2f(relX, relY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}