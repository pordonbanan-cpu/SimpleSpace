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

/** Film-like TARS: 4 tall segments, walk articulates, sprint rolls. */
public class TarsRenderer extends EntityRenderer<TarsEntity> {

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float SW = 0.72f, SH = 0.48f, SD = 0.28f;

    public TarsRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public ResourceLocation getTextureLocation(TarsEntity entity) { return WHITE; }

    @Override
    public void render(TarsEntity entity, float yaw, float pt,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        float walkPos = entity.getWalkAnimPos(pt);
        float walkSpeed = entity.getWalkAnimSpeed();
        boolean sprint = entity.isSprintMode() || walkSpeed > 0.6f;
        float swing = Math.min(1f, walkSpeed * (sprint ? 3.5f : 5f));
        float phase = walkPos * (sprint ? 1.1f : 0.6662f);
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));

        if (swing < 0.08f) renderStanding(pose, vc, light);
        else if (sprint) renderRolling(pose, vc, light, phase);
        else renderWalking(pose, vc, light, phase, swing);

        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    private void renderStanding(PoseStack pose, VertexConsumer vc, int light) {
        for (int i = 0; i < 4; i++) {
            float y = 0.05f + i * SH + SH * 0.5f;
            int shade = 34 + i * 4;
            drawBox(pose, vc, 0, y, 0, SW, SH * 0.96f, SD, shade, shade + 2, shade + 6, light);
        }
        drawBox(pose, vc, 0, 1.0f, -SD * 0.52f, 0.07f, SH * 4 * 0.92f, 0.03f, 230, 185, 45, light);
        drawBox(pose, vc, 0.14f, 1.72f, -SD * 0.52f, 0.08f, 0.08f, 0.03f, 60, 200, 255, light);
    }

    private void renderWalking(PoseStack pose, VertexConsumer vc, int light, float phase, float swing) {
        for (int i = 0; i < 4; i++) {
            float y = 0.05f + i * SH + SH * 0.5f;
            float offset = (i % 2 == 0 ? 1 : -1);
            float z = Mth.sin(phase + i * 0.4f) * 0.18f * swing * offset;
            float pitch = Mth.sin(phase + i * 0.4f) * 22f * swing * offset;
            float yBob = Math.abs(Mth.sin(phase + i * 0.3f)) * 0.04f * swing;
            pose.pushPose();
            pose.translate(0, y + yBob, z);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            int shade = 34 + i * 4;
            drawBox(pose, vc, 0, 0, 0, SW, SH * 0.94f, SD, shade, shade + 2, shade + 6, light);
            if (i == 1 || i == 2)
                drawBox(pose, vc, 0, 0, -SD * 0.52f, 0.07f, SH * 0.85f, 0.03f, 230, 185, 45, light);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0.14f, 1.72f + Math.abs(Mth.sin(phase)) * 0.03f * swing, -SD * 0.52f);
        drawBox(pose, vc, 0, 0, 0, 0.08f, 0.08f, 0.03f, 60, 200, 255, light);
        pose.popPose();
    }

    private void renderRolling(PoseStack pose, VertexConsumer vc, int light, float phase) {
        float roll = phase * 55f;
        pose.translate(0, 0.95f, 0);
        pose.mulPose(Axis.XP.rotationDegrees(roll));
        for (int i = 0; i < 4; i++) {
            float localY = (i - 1.5f) * SH * 0.95f;
            int shade = 34 + i * 4;
            drawBox(pose, vc, 0, localY, 0, SW, SH * 0.92f, SD, shade, shade + 2, shade + 6, light);
            if (i == 1 || i == 2)
                drawBox(pose, vc, 0, localY, -SD * 0.52f, 0.07f, SH * 0.8f, 0.03f, 230, 185, 45, light);
        }
        drawBox(pose, vc, 0.14f, SH * 1.4f, -SD * 0.52f, 0.08f, 0.08f, 0.03f, 60, 200, 255, light);
    }

    private static void drawBox(PoseStack pose, VertexConsumer vc,
                                float cx, float cy, float cz,
                                float w, float h, float d,
                                int r, int g, int b, int light) {
        pose.pushPose();
        pose.translate(cx, cy, cz);
        Matrix4f m = pose.last().pose();
        float x0 = -w / 2, x1 = w / 2, y0 = -h / 2, y1 = h / 2, z0 = -d / 2, z1 = d / 2;
        face(vc, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b, light);
        face(vc, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, r,g,b, light);
        face(vc, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b, light);
        face(vc, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, r,g,b, light);
        face(vc, m, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, r,g,b, light);
        face(vc, m, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, r,g,b, light);
        pose.popPose();
    }

    private static void face(VertexConsumer vc, Matrix4f m,
                             float x0,float y0,float z0, float x1,float y1,float z1,
                             float x2,float y2,float z2, float x3,float y3,float z3,
                             int r,int g,int b,int light) {
        v(vc,m,x0,y0,z0,r,g,b,light); v(vc,m,x1,y1,z1,r,g,b,light);
        v(vc,m,x2,y2,z2,r,g,b,light); v(vc,m,x3,y3,z3,r,g,b,light);
    }

    private static void v(VertexConsumer vc, Matrix4f m,
                          float x, float y, float z, int r, int g, int b, int light) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, 255).setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }
}
