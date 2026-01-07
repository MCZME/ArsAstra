package com.github.mczme.arsastra.client.gui.widget;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.AAClientEvents;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * 星图渲染工具类
 * 负责处理底层的几何生成、Tesselator 操作以及达芬奇手绘风格的绘制。
 */
public class StarChartRenderUtils {

    // 纹理资源定义
    public static final ResourceLocation PARCHMENT_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/parchment_background.png");
    public static final ResourceLocation HATCHING_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/hatching.png");
    public static final ResourceLocation INK_WASH_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/ink_wash.png");

    /**
     * 炼金术古典色板
     */
    public static class Palette {
        public static final int CINNABAR = 0xFFA52A2A;  // 朱砂/赭石 (红)
        public static final int INDIGO = 0xFF081D40;    // 靛青/普鲁士蓝 (蓝)
        public static final int MALACHITE = 0xFF0BDA51; // 孔雀石绿 (绿)
        public static final int INK = 0xFF1A1714;      // 铁胆墨色 (主线条颜色)
        public static final int PAPER = 0xFFE6D5AC;    // 陈年纸黄色
        
        public static final int MANA = INDIGO;
        public static final int LIFE = CINNABAR;
        public static final int NATURE = MALACHITE;
    }

    // --- 纹理与视差层渲染 ---

    public static void drawParallaxLayer(PoseStack poseStack, float x, float y, float w, float h, float offsetX, float offsetY, float parallax, float textureSize, ResourceLocation texture, boolean multiply) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        
        if (multiply) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
        }

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = poseStack.last().pose();
        
        float uMin = (-offsetX * parallax) / textureSize;
        float vMin = (-offsetY * parallax) / textureSize;
        float uMax = uMin + (w / textureSize);
        float vMax = vMin + (h / textureSize);
        
        buffer.addVertex(matrix, x, y + h, 0).setUv(uMin, vMax);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(uMax, vMax);
        buffer.addVertex(matrix, x + w, y, 0).setUv(uMax, vMin);
        buffer.addVertex(matrix, x, y, 0).setUv(uMin, vMin);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    // --- Stencil 遮罩管理 ---

    public static void beginStencilMask() {
        // 确保主 Framebuffer 启用了 Stencil
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.colorMask(false, false, false, false);
    }

    public static void applyStencilMask() {
        RenderSystem.colorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public static void endStencilMask() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    // --- 全局排线渲染 ---

    public static void drawGlobalHatching(PoseStack poseStack, float x, float y, float w, float h, float worldScale, float worldOffsetX, float worldOffsetY, int color) {
        ShaderInstance shader = AAClientEvents.getDavinciHatchingShader();
        if (shader == null) return;

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, HATCHING_TEXTURE);
        
        if (shader.getUniform("InkColor") != null) {
            float r = (color >> 16 & 255) / 255.0f;
            float g = (color >> 8 & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            float a = (color >> 24 & 255) / 255.0f;
            shader.getUniform("InkColor").set(r, g, b, a);
        }
        if (shader.getUniform("UVScale") != null) shader.getUniform("UVScale").set(worldScale);
        if (shader.getUniform("UVOffset") != null) shader.getUniform("UVOffset").set(worldOffsetX, worldOffsetY);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        buffer.addVertex(matrix, x, y + h, 0).setUv(0, 1).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(1, 1).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x + w, y, 0).setUv(1, 0).setColor(1f, 1f, 1f, 1f);
        buffer.addVertex(matrix, x, y, 0).setUv(0, 0).setColor(1f, 1f, 1f, 1f);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    // --- 湿画法上色 (后期处理) ---

    /**
     * 绘制具有“咖啡环效应”的水彩形状
     */
    public static void drawWaterColorShape(PoseStack poseStack, Runnable shapeDrawer, int color, float intensity) {
        // 1. 启用 Stencil，清空
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        
        // 2. 绘制底色 (同时写入 Stencil)
        // 使用 PositionShader (shapeDrawer 内部调用 drawSolidPolygon/Circle)
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
            ((color >> 16) & 255) / 255f,
            ((color >> 8) & 255) / 255f,
            (color & 255) / 255f,
            intensity * 0.4f
        );
        shapeDrawer.run();
        
        // 3. 切换到“仅在 Stencil 区域绘制”
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        
        // 4. 绘制噪点纹理 (Multiply 混合，模拟纸张纹理叠加)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, INK_WASH_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // 纹理层稍微深一点，模拟边缘堆积
        RenderSystem.setShaderColor(0.8f, 0.8f, 0.8f, intensity * 0.8f); 
        
        float big = 5000.0f; // 覆盖足够大的区域
        float uvScale = 0.005f; // 纹理缩放因子
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = poseStack.last().pose();
        
        buffer.addVertex(matrix, -big, big, 0).setUv(0, big * uvScale);
        buffer.addVertex(matrix, big, big, 0).setUv(big * uvScale, big * uvScale);
        buffer.addVertex(matrix, big, -big, 0).setUv(big * uvScale, 0);
        buffer.addVertex(matrix, -big, -big, 0).setUv(0, 0);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        // 5. 恢复状态
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    // --- 基础几何体绘制 ---

    public static void drawSolidCircle(PoseStack poseStack, Vector2f center, float radius) {
        int segments = 64;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        Matrix4f matrix = poseStack.last().pose();

        buffer.addVertex(matrix, center.x, center.y, 0);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            buffer.addVertex(matrix, center.x + (float)Math.cos(angle) * radius, center.y + (float)Math.sin(angle) * radius, 0);
        }
        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawSolidPolygon(PoseStack poseStack, List<Vector2f> vertices) {
        if (vertices.size() < 3) return;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        Matrix4f matrix = poseStack.last().pose();

        for (Vector2f vertex : vertices) {
            buffer.addVertex(matrix, vertex.x, vertex.y, 0);
        }
        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    // --- 动态一致性与 LOD --- 

    /**
     * 绘制达芬奇风格的多边形内蚀排线
     * 使用 Mesh 构建方案，通过顶点颜色插值实现边缘淡出
     * @param points 多边形顶点列表（应为闭合路径点）
     * @param fadeWidth 淡出带的宽度（像素）
     * @param color 墨水颜色（包含 Alpha 权重）
     */
    public static void drawDaVinciHatchedPolygon(PoseStack poseStack, List<Vector2f> points, float fadeWidth, int color, float uvScale, float offsetX, float offsetY) {
        ShaderInstance shader = AAClientEvents.getDavinciHatchingShader();
        if (shader == null || points.size() < 3) return;

        // 计算中心点（用于计算收缩方向）
        Vector2f center = new Vector2f(0, 0);
        for (Vector2f p : points) {
            center.add(p);
        }
        center.div(points.size());

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, HATCHING_TEXTURE);
        
        if (shader.getUniform("InkColor") != null) {
            float r = (color >> 16 & 255) / 255.0f;
            float g = (color >> 8 & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            float a = (color >> 24 & 255) / 255.0f;
            shader.getUniform("InkColor").set(r, g, b, a);
        }
        if (shader.getUniform("UVScale") != null) shader.getUniform("UVScale").set(uvScale);
        if (shader.getUniform("UVOffset") != null) shader.getUniform("UVOffset").set(offsetX, offsetY);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        
        // 绘制环带
        for (int i = 0; i <= points.size(); i++) {
            Vector2f pOuter = points.get(i % points.size());
            
            // 计算向中心收缩的内点
            Vector2f dir = new Vector2f(center).sub(pOuter);
            float dist = dir.length();
            Vector2f pInner;
            if (dist > fadeWidth) {
                pInner = new Vector2f(dir).normalize().mul(fadeWidth).add(pOuter);
            } else {
                pInner = new Vector2f(center);
            }

            // 外边缘顶点 (Alpha = 1)
            buffer.addVertex(matrix, pOuter.x, pOuter.y, 0).setUv(0, 0).setColor(r, g, b, 1.0f);
            // 内边缘顶点 (Alpha = 0)
            buffer.addVertex(matrix, pInner.x, pInner.y, 0).setUv(0, 0).setColor(r, g, b, 0.0f);
        }
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static float getScaleCompensatedWidth(float targetPixelWidth, float currentScale) {
        float safeScale = Math.max(0.01f, currentScale);
        return Math.min(targetPixelWidth / safeScale, 50.0f);
    }

    public enum LODLevel { MACRO, NORMAL, DETAIL }

    public static LODLevel getLODLevel(float scale) {
        if (scale < 0.2f) return LODLevel.MACRO;
        if (scale < 1.5f) return LODLevel.NORMAL;
        return LODLevel.DETAIL;
    }

    // --- 测绘风格图元 ---

    public static void drawSurveyCircle(PoseStack poseStack, Vector2f center, float radius, int color, float baseWidth) {
        // 辅环
        drawRibbonCircle(poseStack, center, radius + 2.0f, baseWidth * 0.8f, color, 0.2f);
        // 主环
        drawDynamicCircle(poseStack, center, radius, color, baseWidth);
        // 针孔
        drawSolidCircle(poseStack, center, baseWidth * 0.6f);
        
        // 十字准星
        float crossSize = 3.0f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        float r = (color >> 16 & 255) / 255.0f, g = (color >> 8 & 255) / 255.0f, b = (color & 255) / 255.0f;
        
        buffer.addVertex(matrix, center.x - crossSize, center.y, 0).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, center.x + crossSize, center.y, 0).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, center.x, center.y - crossSize, 0).setColor(r, g, b, 0.3f);
        buffer.addVertex(matrix, center.x, center.y + crossSize, 0).setColor(r, g, b, 0.3f);
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    // --- 动态描边与图标渲染 ---

    public static void drawDynamicLoop(PoseStack poseStack, List<Vector2f> vertices, int color, float baseWidth) {
        if (vertices.size() < 2) return;
        drawRibbonPath(poseStack, vertices, true, baseWidth * 1.8f, color, 0.4f);
        drawRibbonPath(poseStack, vertices, true, baseWidth, color, 0.9f);
    }

    public static void drawDynamicCircle(PoseStack poseStack, Vector2f center, float radius, int color, float baseWidth) {
        drawRibbonCircle(poseStack, center, radius, baseWidth * 1.8f, color, 0.4f);
        drawRibbonCircle(poseStack, center, radius, baseWidth, color, 0.9f);
    }

    public static void drawMonochromeIcon(PoseStack poseStack, TextureAtlasSprite sprite, Vector2f center, float size, int color) {
        if (sprite == null) return;
        var shader = AAClientEvents.getMonochromeShader();
        RenderSystem.setShader(() -> shader != null ? shader : GameRenderer.getPositionTexColorShader());
        
        if (shader != null && shader.getUniform("InkColor") != null) {
            float r = (color >> 16 & 255) / 255.0f, g = (color >> 8 & 255) / 255.0f, b = (color & 255) / 255.0f, a = (color >> 24 & 255) / 255.0f;
            shader.getUniform("InkColor").set(r, g, b, a);
        }

        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        float x = center.x - size / 2.0f, y = center.y - size / 2.0f;
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        buffer.addVertex(matrix, x, y + size, 0).setUv(u0, v1).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(matrix, x + size, y + size, 0).setUv(u1, v1).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(matrix, x + size, y, 0).setUv(u1, v0).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(matrix, x, y, 0).setUv(u0, v0).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawPath(PoseStack poseStack, List<Vector2f> path, float width, int color) {
        if (path == null || path.size() < 2) return;
        drawRibbonPath(poseStack, path, false, width, color, 1.0f);
    }

    // --- 内部辅助方法 (Ribbon AA 渲染逻辑) ---

    private static void drawRibbonPath(PoseStack poseStack, List<Vector2f> vertices, boolean closed, float width, int color, float alphaMult) {
        if (vertices.size() < 2) return;
        
        Tesselator tesselator = Tesselator.getInstance();
        // 改用 TRIANGLES 模式，手动构建每个线段的几何体
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float a = ((color >> 24 & 255) / 255.0f) * alphaMult;
        
        int count = closed ? vertices.size() : vertices.size() - 1;
        
        for (int i = 0; i < count; i++) {
            Vector2f p1 = vertices.get(i);
            Vector2f p2 = vertices.get((i + 1) % vertices.size());
            
            // 计算当前线段的准确法线 (垂直于 p1->p2)
            Vector2f dir = new Vector2f(p2).sub(p1).normalize();
            float nx = -dir.y;
            float ny = dir.x;
            
            // 绘制线段主体 (Segment Body)
            addSegmentVertices(buffer, matrix, p1, p2, nx, ny, width, r, g, b, a);
            
            // 绘制连接点 (Join) - 简单的实心圆点覆盖接缝
            // 使用 p2 作为连接点 (对于最后一个点，如果是闭合的也会覆盖到)
            // 半径略大于线宽的一半以确保覆盖
            addJoinVertices(buffer, matrix, p2, width * 0.55f, r, g, b, a);
        }
        
        // 如果不闭合，还需要处理起点的圆头 (终点在循环中作为 Join 处理了，除了非闭合的最后一段的末端)
        if (!closed) {
             addJoinVertices(buffer, matrix, vertices.get(0), width * 0.55f, r, g, b, a);
             addJoinVertices(buffer, matrix, vertices.get(vertices.size() - 1), width * 0.55f, r, g, b, a);
        } else {
            // 闭合路径循环中已经覆盖了所有连接点，但起点(也是终点)可能需要额外确保一下
             addJoinVertices(buffer, matrix, vertices.get(0), width * 0.55f, r, g, b, a);
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
    
    // 添加线段的几何体 (3个 Quad: 左渐变, 中心实体, 右渐变)
    private static void addSegmentVertices(BufferBuilder buffer, Matrix4f matrix, Vector2f p1, Vector2f p2, float nx, float ny, float width, float r, float g, float b, float a) {
        float halfW = width * 0.5f;
        float coreW = halfW * 0.5f; // 核心实心部分的半宽
        
        // 预计算偏移向量
        float dxCore = nx * coreW;
        float dyCore = ny * coreW;
        float dxEdge = nx * halfW;
        float dyEdge = ny * halfW;
        
        // Quad 1: 左侧渐变 (EdgeL -> CoreL)
        // P1 EdgeL -> P1 CoreL -> P2 CoreL -> P2 EdgeL
        addQuad(buffer, matrix, 
            p1.x + dxEdge, p1.y + dyEdge, 0.0f,
            p1.x + dxCore, p1.y + dyCore, a,
            p2.x + dxCore, p2.y + dyCore, a,
            p2.x + dxEdge, p2.y + dyEdge, 0.0f,
            r, g, b
        );
        
        // Quad 2: 中心实体 (CoreL -> CoreR)
        addQuad(buffer, matrix, 
            p1.x + dxCore, p1.y + dyCore, a,
            p1.x - dxCore, p1.y - dyCore, a,
            p2.x - dxCore, p2.y - dyCore, a,
            p2.x + dxCore, p2.y + dyCore, a,
            r, g, b
        );

        // Quad 3: 右侧渐变 (CoreR -> EdgeR)
        addQuad(buffer, matrix, 
            p1.x - dxCore, p1.y - dyCore, a,
            p1.x - dxEdge, p1.y - dyEdge, 0.0f,
            p2.x - dxEdge, p2.y - dyEdge, 0.0f,
            p2.x - dxCore, p2.y - dyCore, a,
            r, g, b
        );
    }
    
    // 简单的圆形 Join (使用扇形构建)
    private static void addJoinVertices(BufferBuilder buffer, Matrix4f matrix, Vector2f center, float radius, float r, float g, float b, float a) {
        int segments = 12; // 连接处不需要太多段数
        for (int i = 0; i < segments; i++) {
            float ang1 = (float) (i * 2 * Math.PI / segments);
            float ang2 = (float) ((i + 1) * 2 * Math.PI / segments);
            
            float x1 = center.x + (float)Math.cos(ang1) * radius;
            float y1 = center.y + (float)Math.sin(ang1) * radius;
            float x2 = center.x + (float)Math.cos(ang2) * radius;
            float y2 = center.y + (float)Math.sin(ang2) * radius;
            
            // 中心点到外圆周的三角形
            // 中心点 alpha = a, 边缘 alpha = 0 (模拟笔触羽化) 或者 保持实心
            // 这里为了模拟圆头笔，中心最黑，边缘渐变
            float edgeAlpha = 0.0f; 
            
            buffer.addVertex(matrix, center.x, center.y, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, edgeAlpha);
            buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, edgeAlpha);
        }
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, 
                                float x1, float y1, float a1,
                                float x2, float y2, float a2,
                                float x3, float y3, float a3,
                                float x4, float y4, float a4,
                                float r, float g, float b) {
        // 拆分成两个三角形: (1, 2, 3) 和 (1, 3, 4)
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a1);
        buffer.addVertex(matrix, x2, y2, 0).setColor(r, g, b, a2);
        buffer.addVertex(matrix, x3, y3, 0).setColor(r, g, b, a3);
        
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a1);
        buffer.addVertex(matrix, x3, y3, 0).setColor(r, g, b, a3);
        buffer.addVertex(matrix, x4, y4, 0).setColor(r, g, b, a4);
    }

    private static void drawRibbonCircle(PoseStack poseStack, Vector2f center, float radius, float width, int color, float alphaMult) {
        int segments = 64;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;
        float a = ((color >> 24 & 255) / 255.0f) * alphaMult;
        
        for (int i = 0; i < segments; i++) {
            float ang1 = (float) (i * 2 * Math.PI / segments);
            float ang2 = (float) ((i + 1) * 2 * Math.PI / segments);
            
            float cos1 = (float)Math.cos(ang1), sin1 = (float)Math.sin(ang1);
            float cos2 = (float)Math.cos(ang2), sin2 = (float)Math.sin(ang2);
            
            Vector2f p1 = new Vector2f(center.x + cos1 * radius, center.y + sin1 * radius);
            Vector2f p2 = new Vector2f(center.x + cos2 * radius, center.y + sin2 * radius);
            
            // 圆的法线就是径向向量 (p - center).normalize()，或者直接用 (cos, sin)
            // 对于每一段，我们可以近似取中点的法线，或者分别用两个端点的法线插值 (太复杂)
            // 这里为了简单且一致，使用线段的垂直向量作为法线 (同 drawRibbonPath)
            Vector2f dir = new Vector2f(p2).sub(p1).normalize();
            float nx = -dir.y;
            float ny = dir.x;
            
            addSegmentVertices(buffer, matrix, p1, p2, nx, ny, width, r, g, b, a);
            
            // 绘制 Join (连接点) 填补缝隙
            // 使用 p2 作为连接点
            addJoinVertices(buffer, matrix, p2, width * 0.55f, r, g, b, a);
        }
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addAAVertices(BufferBuilder buffer, Matrix4f matrix, float x, float y, float nx, float ny, float width, float r, float g, float b, float a) {
        float halfW = width * 0.5f;
        float coreW = halfW * 0.5f; 
        
        buffer.addVertex(matrix, x + nx * halfW, y + ny * halfW, 0).setColor(r, g, b, 0.0f);
        buffer.addVertex(matrix, x + nx * coreW, y + ny * coreW, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x - nx * coreW, y - ny * coreW, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x - nx * halfW, y - ny * halfW, 0).setColor(r, g, b, 0.0f);
    }
}