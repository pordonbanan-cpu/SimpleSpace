package com.simplespace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplespace.tars.TarsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class TarsRenderer extends EntityRenderer<TarsEntity> {

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public TarsRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(TarsEntity entity) {
        return WHITE;
    }

    @Override
    public void render(TarsEntity entity, float yaw, float pt,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();

        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        pose.translate(0, 0.05, 0);

        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));

        float[][] modules = {
                {0.85f, 0.45f, 0.35f, 0.00f},
                {0.90f, 0.50f, 0.38f, 0.48f},
                {0.88f, 0.48f, 0.36f, 1.00f},
                {0.70f, 0.40f, 0.32f, 1.50f},
        };

        for (float[] m : modules) {
            drawBox(pose, vc, 0, m[3] + m[1] / 2f, 0, m[0], m[1], m[2],
                    35, 38, 42, light);
        }

        drawBox(pose, vc, 0, 0.95f, -0.20f, 0.12f, 1.6f, 0.04f,
                220, 180, 40, light);

        drawBox(pose, vc, 0.15f, 1.55f, -0.19f, 0.10f, 0.10f, 0.05f,
                80, 200, 255, light);

        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    private static void drawBox(PoseStack pose, VertexConsumer vc,
                                float cx, float cy, float cz,
                                float w, float h, float d,
                                int r, int g, int b, int light) {
        pose.pushPose();
        pose.translate(cx, cy, cz);
        Matrix4f mat = pose.last().pose();
        float x0 = -w / 2, x1 = w / 2;
        float y0 = -h / 2, y1 = h / 2;
        float z0 = -d / 2, z1 = d / 2;

        quad(vc, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, light);
        quad(vc, mat, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, light);
        quad(vc, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, light);
        quad(vc, mat, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, light);
        quad(vc, mat, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r, g, b, light);
        quad(vc, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, light);

        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int b, int light) {
        vert(vc, mat, x0, y0, z0, r, g, b, light);
        vert(vc, mat, x1, y1, z1, r, g, b, light);
        vert(vc, mat, x2, y2, z2, r, g, b, light);
        vert(vc, mat, x0, y0, z0, r, g, b, light);
        vert(vc, mat, x2, y2, z2, r, g, b, light);
        vert(vc, mat, x3, y3, z3, r, g, b, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f mat,
                             float x, float y, float z, int r, int g, int b, int light) {
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}
