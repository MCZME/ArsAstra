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

import java.util.List;

/**
 * 星图渲染工具类
 * 负责处理底层的几何生成、Tesselator 操作以及手绘风格的绘制。
 */
public class StarChartRenderUtils {

    // 纹理资源定义
    public static final ResourceLocation PARCHMENT_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/parchment_background.png");
    public static final ResourceLocation HATCHING_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID, "textures/gui/hatching.png");

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
     * 改用 Miter Offset 算法以正确处理凹多边形
     * @param points 多边形顶点列表（应为闭合路径点）
     * @param fadeWidth 淡出带的宽度（像素）
     * @param color 墨水颜色（包含 Alpha 权重）
     */
    public static void drawDaVinciHatchedPolygon(PoseStack poseStack, List<Vector2f> points, float fadeWidth, int color, float uvScale, float offsetX, float offsetY) {
        ShaderInstance shader = AAClientEvents.getDavinciHatchingShader();
        if (shader == null || points.size() < 3) return;

        // 1. 检测卷绕方向，确保向“内”偏移
        boolean isClockwise = isClockwise(points);
        // 修正：根据测试反馈，GUI坐标系下需要反转系数以指向内部
        float orientation = isClockwise ? -1.0f : 1.0f;

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
        
        int count = points.size();
        for (int i = 0; i <= count; i++) {
            int currIdx = i % count;
            int prevIdx = (i - 1 + count) % count;
            int nextIdx = (i + 1) % count;

            Vector2f pCurr = points.get(currIdx);
            Vector2f pPrev = points.get(prevIdx);
            Vector2f pNext = points.get(nextIdx);

            // 计算相邻两条边的切线向量
            Vector2f t1 = new Vector2f(pCurr).sub(pPrev).normalize();
            Vector2f t2 = new Vector2f(pNext).sub(pCurr).normalize();

            // 计算法线 (旋转90度)
            Vector2f n1 = new Vector2f(-t1.y * orientation, t1.x * orientation);
            Vector2f n2 = new Vector2f(-t2.y * orientation, t2.x * orientation);
            
            // 安全检查：如果向量无效 (NaN)，使用默认值
            if (Float.isNaN(n1.x) || Float.isNaN(n1.y)) n1.set(0, 0);
            if (Float.isNaN(n2.x) || Float.isNaN(n2.y)) n2.set(0, 0);

            // 计算 Miter 方向 (平均法线)
            Vector2f miter = new Vector2f(n1).add(n2).normalize();
            if (Float.isNaN(miter.x) || Float.isNaN(miter.y)) miter.set(n1); // Fallback

            // 计算长度缩放因子 (1 / dot(miter, n1))
            float dot = miter.dot(n1);
            float limit = 3.0f; 
            // 强制取绝对值，防止符号反转导致方向错误
            float lengthScale = Math.abs((Math.abs(dot) < 1.0f / limit) ? limit : 1.0f / dot);
            
            float offsetLen = fadeWidth * lengthScale;
            Vector2f pInner = new Vector2f(miter).mul(offsetLen).add(pCurr);

            // 外边缘顶点 (Alpha = 1)
            buffer.addVertex(matrix, pCurr.x, pCurr.y, 0).setUv(0, 0).setColor(r, g, b, 1.0f);
            // 内边缘顶点 (Alpha = 0)
            buffer.addVertex(matrix, pInner.x, pInner.y, 0).setUv(0, 0).setColor(r, g, b, 0.0f);
        }
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static boolean isClockwise(List<Vector2f> points) {
        float sum = 0.0f;
        for (int i = 0; i < points.size(); i++) {
            Vector2f p1 = points.get(i);
            Vector2f p2 = points.get((i + 1) % points.size());
            sum += (p2.x - p1.x) * (p2.y + p1.y);
        }
        return sum > 0;
    }

    /**
     * 使用排线纹理填充整个多边形内部
     * 使用 Stencil 奇偶规则 (Even-Odd) 以支持凹多边形
     */
    public static void drawHatchedPolygonFilled(PoseStack poseStack, List<Vector2f> points, int color, float uvScale, float offsetX, float offsetY) {
        if (points.size() < 3) return;

        // 1. 准备 Stencil 环境
        // 必须显式启用 Stencil Buffer，否则 GL_STENCIL_TEST 无效
        Minecraft.getInstance().getMainRenderTarget().enableStencil();
        
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT); // 清除当前 Stencil
        GL11.glStencilMask(0xFF); // 允许写入

        // 2. 绘制遮罩 (使用奇偶规则)
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT); // 核心：反转位，实现奇偶填充

        // 使用 FAN 绘制几何体到 Stencil
        // 只要顶点顺序是连续的，FAN + INVERT 可以正确处理任意非自交多边形
        RenderSystem.setShader(GameRenderer::getPositionShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        Matrix4f matrix = poseStack.last().pose();
        
        // 使用第一个点作为 FAN 的中心 (对于 INVERT 规则，中心点在哪里不重要，只要在平面上)
        buffer.addVertex(matrix, points.get(0).x, points.get(0).y, 0);
        for (int i = 1; i < points.size(); i++) {
            buffer.addVertex(matrix, points.get(i).x, points.get(i).y, 0);
        }
        // 闭合
        buffer.addVertex(matrix, points.get(1).x, points.get(1).y, 0); 
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // 3. 绘制排线填充 (仅在 Stencil == 1 的区域)
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        // 使用 NOTEQUAL 0，因为 INVERT 操作会将 0 变为 0xFF (255)，而不是 1
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        // 计算包围盒以限制绘制区域
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Vector2f p : points) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
        }

        // 调用 Shader 绘制排线矩形
        ShaderInstance shader = AAClientEvents.getDavinciHatchingShader();
        if (shader != null) {
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

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);

            buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            
            buffer.addVertex(matrix, minX, maxY, 0).setUv(0, 1).setColor(1f, 1f, 1f, 1f);
            buffer.addVertex(matrix, maxX, maxY, 0).setUv(1, 1).setColor(1f, 1f, 1f, 1f);
            buffer.addVertex(matrix, maxX, minY, 0).setUv(1, 0).setColor(1f, 1f, 1f, 1f);
            buffer.addVertex(matrix, minX, minY, 0).setUv(0, 0).setColor(1f, 1f, 1f, 1f);

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.defaultBlendFunc();
        }

        // 4. 清理状态
        GL11.glDisable(GL11.GL_STENCIL_TEST);
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
}