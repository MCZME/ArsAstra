package com.github.mczme.arsastra.client.gui.util;

import com.github.mczme.arsastra.ArsAstra;
import com.github.mczme.arsastra.client.AAClientEvents;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * 路径渲染器
 * 专门用于处理基于纹理的路径渲染，例如铅笔风格。
 */
public class PathRenderer {

    public static final ResourceLocation PENCIL_TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsAstra.MODID,
            "textures/gui/pencil_texture.png");

    /**
     * 渲染一条带纹理的路径。
     *
     * @param poseStack 矩阵栈
     * @param points    路径点集
     * @param width     路径宽度
     * @param color     颜色 (ARGB) - 在正片叠底模式下，建议使用白色，否则会染黑背景
     * @param textureScale 纹理缩放系数 (控制纹理沿路径的重复频率，值越小重复越密)
     */
    public static void renderPencilPath(PoseStack poseStack, List<Vector2f> points, float width, int color, float textureScale) {
        if (points == null || points.size() < 2) return;

        RenderSystem.setShaderTexture(0, PENCIL_TEXTURE);
        // 确保纹理模式为 REPEAT
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(PENCIL_TEXTURE);
        if (texture != null) {
            texture.bind();
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }

        // 尝试使用自定义铅笔 Shader，实现像素级的边缘粗糙化
        ShaderInstance shader = AAClientEvents.getPencilPathShader();
        if (shader != null) {
            RenderSystem.setShader(() -> shader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        }
        
        RenderSystem.enableBlend();
        // 使用正片叠底混合模式：DST_COLOR * SRC (这里的 SRC 是纹理 * 顶点色)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        // 强制使用白色顶点色，以免染黑背景
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;

        float totalDistance = 0;
        float halfWidth = width / 2.0f;

        int count = points.size();
        
        for (int i = 0; i < count; i++) {
            Vector2f curr = points.get(i);
            
            // 寻找有效的前驱点 (跳过重合点)
            Vector2f prevValid = null;
            for (int j = i - 1; j >= 0; j--) {
                Vector2f p = points.get(j);
                if (curr.distanceSquared(p) > 1e-6) {
                    prevValid = p;
                    break;
                }
            }

            // 寻找有效的后继点 (跳过重合点)
            Vector2f nextValid = null;
            for (int j = i + 1; j < count; j++) {
                Vector2f p = points.get(j);
                if (curr.distanceSquared(p) > 1e-6) {
                    nextValid = p;
                    break;
                }
            }

            // 计算当前点的切线方向
            Vector2f tangent = new Vector2f();
            boolean hasTangent = false;

            if (prevValid != null && nextValid != null) {
                // 中间点：平均切线
                Vector2f dir1 = new Vector2f(curr).sub(prevValid).normalize();
                Vector2f dir2 = new Vector2f(nextValid).sub(curr).normalize();
                tangent.set(dir1).add(dir2);
                if (tangent.lengthSquared() > 1e-6) {
                    tangent.normalize();
                    hasTangent = true;
                }
            } 
            
            if (!hasTangent) {
                if (nextValid != null) {
                    // 起点或只有后继
                    tangent.set(nextValid).sub(curr).normalize();
                    hasTangent = true;
                } else if (prevValid != null) {
                    // 终点或只有前驱
                    tangent.set(curr).sub(prevValid).normalize();
                    hasTangent = true;
                }
            }

            // 如果仍然无法计算切线 (例如所有点都重合)，使用默认值防止 NaN
            if (!hasTangent) {
                tangent.set(1.0f, 0.0f);
            }

            // 法线：切线顺时针旋转 90 度 (-y, x) 或 逆时针 (y, -x)
            // 这里取 (-y, x)
            Vector2f normal = new Vector2f(-tangent.y, tangent.x);

            float miterLength = halfWidth;

            // 累加距离用于 UV 计算
            if (i > 0) {
                totalDistance += curr.distance(points.get(i - 1));
            }

            float u = totalDistance / (width * textureScale);

            // Shader 会处理边缘粗糙化，这里直接使用标准宽度
            // 添加两个顶点 (左边和右边)
            // 左点 (V=0)
            buffer.addVertex(matrix, curr.x + normal.x * miterLength, curr.y + normal.y * miterLength, 0)
                  .setUv(u, 0.0f)
                  .setColor(r, g, b, a);

            // 右点 (V=1)
            buffer.addVertex(matrix, curr.x - normal.x * miterLength, curr.y - normal.y * miterLength, 0)
                  .setUv(u, 1.0f)
                  .setColor(r, g, b, a);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        // 恢复默认混合模式，以免影响后续渲染
        RenderSystem.defaultBlendFunc();
    }
}
