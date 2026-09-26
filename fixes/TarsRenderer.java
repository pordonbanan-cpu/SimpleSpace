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
        this.shadowRadius = 0.45f;
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

        float walkPos = entity.getWalkAnimPos(pt);
        float walkSpeed = entity.getWalkAnimSpeed();
        float swing = Math.min(1f, walkSpeed * 4f);
        float phase = walkPos * 0.6662f;

        float leftZ = Mth.sin(phase) * 0.22f * swing;
        float rightZ = Mth.sin(phase + Mth.PI) * 0.22f * swing;
        float leftY = Math.max(0, Mth.sin(phase)) * 0.08f * swing;
        float rightY = Math.max(0, Mth.sin(phase + Mth.PI)) * 0.08f * swing;
        float leftRot = Mth.sin(phase) * 18f * swing;
        float rightRot = Mth.sin(phase + Mth.PI) * 18f * swing;

        float bob = Mth.sin(phase * 2f) * 0.03f * swing;
        pose.translate(0, 0.02f + bob, 0);

        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));

        drawBox(pose, vc, 0, 0.35f, 0, 0.72f, 0.55f, 0.40f, 40, 42, 48, light);
        drawBox(pose, vc, 0, 0.95f, 0, 0.78f, 0.55f, 0.42f, 36, 38, 44, light);
        drawBox(pose, vc, 0, 1.55f, 0, 0.62f, 0.42f, 0.36f, 32, 34, 40, light);
        drawBox(pose, vc, 0, 0.70f, -0.22f, 0.10f, 1.15f, 0.04f, 230, 190, 50, light);
        drawBox(pose, vc, 0.12f, 1.55f, -0.20f, 0.09f, 0.09f, 0.04f, 70, 210, 255, light);

        pose.pushPose();
        pose.translate(-0.55f, 0.15f + leftY, leftZ);
        pose.mulPose(Axis.XP.rotationDegrees(leftRot));
        drawBox(pose, vc, 0, 0.35f, 0, 0.32f, 0.70f, 0.32f, 28, 30, 36, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.55f, 0.15f + rightY, rightZ);
        pose.mulPose(Axis.XP.rotationDegrees(rightRot));
        drawBox(pose, vc, 0, 0.35f, 0, 0.32f, 0.70f, 0.32f, 28, 30, 36, light);
        pose.popPose();

        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    private static void drawBox(PoseStack pose, VertexConsumer vc,
                                float cx, float cy, float cz,
                                float w, float h, float d,
                                int r, int g, int b, int light) {
        pose.pushPose();
        pose.translate(cx, cy, cz);
        Matrix4f m = pose.last().pose();
        float x0 = -w / 2, x1 = w / 2;
        float y0 = -h / 2, y1 = h / 2;
        float z0 = -d / 2, z1 = d / 2;

        v(vc, m, x0, y0, z1, r, g, b, light);
        v(vc, m, x1, y0, z1, r, g, b, light);
        v(vc, m, x1, y1, z1, r, g, b, light);
        v(vc, m, x0, y1, z1, r, g, b, light);

        v(vc, m, x1, y0, z0, r, g, b, light);
        v(vc, m, x0, y0, z0, r, g, b, light);
        v(vc, m, x0, y1, z0, r, g, b, light);
        v(vc, m, x1, y1, z0, r, g, b, light);

        v(vc, m, x0, y0, z0, r, g, b, light);
        v(vc, m, x0, y0, z1, r, g, b, light);
        v(vc, m, x0, y1, z1, r, g, b, light);
        v(vc, m, x0, y1, z0, r, g, b, light);

        v(vc, m, x1, y0, z1, r, g, b, light);
        v(vc, m, x1, y0, z0, r, g, b, light);
        v(vc, m, x1, y1, z0, r, g, b, light);
        v(vc, m, x1, y1, z1, r, g, b, light);

        v(vc, m, x0, y1, z1, r, g, b, light);
        v(vc, m, x1, y1, z1, r, g, b, light);
        v(vc, m, x1, y1, z0, r, g, b, light);
        v(vc, m, x0, y1, z0, r, g, b, light);

        v(vc, m, x0, y0, z0, r, g, b, light);
        v(vc, m, x1, y0, z0, r, g, b, light);
        v(vc, m, x1, y0, z1, r, g, b, light);
        v(vc, m, x0, y0, z1, r, g, b, light);

        pose.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m,
                          float x, float y, float z, int r, int g, int b, int light) {
        vc.addVertex(m, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(0f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}
