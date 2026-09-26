package com.simplespace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplespace.entity.CelestialBodyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class CelestialBodyRenderer extends EntityRenderer<CelestialBodyEntity> {

    private static final ResourceLocation SUN =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/sun.png");
    private static final ResourceLocation EARTH =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/earth.png");
    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/moon.png");

    public CelestialBodyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(CelestialBodyEntity entity) {
        String name = entity.getType().toShortString();
        if (name.contains("sun")) return SUN;
        if (name.contains("earth")) return EARTH;
        return MOON;
    }

    @Override
    public void render(CelestialBodyEntity entity, float yaw, float pt,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();

        float radius = entity.getBbWidth() * 0.5f;
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(radius * 2f, radius * 2f, 1f);

        ResourceLocation tex = getTextureLocation(entity);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        int fullBright = 0xF000F0;
        Matrix4f mat = pose.last().pose();

        vert(vc, mat, -0.5f, -0.5f, 0, 0, 1, fullBright);
        vert(vc, mat,  0.5f, -0.5f, 0, 1, 1, fullBright);
        vert(vc, mat,  0.5f,  0.5f, 0, 1, 0, fullBright);

        vert(vc, mat, -0.5f, -0.5f, 0, 0, 1, fullBright);
        vert(vc, mat,  0.5f,  0.5f, 0, 1, 0, fullBright);
        vert(vc, mat, -0.5f,  0.5f, 0, 0, 0, fullBright);

        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f mat,
                             float x, float y, float z, float u, float v, int light) {
        vc.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0, 0, 1);
    }

    @Override
    public boolean shouldRender(CelestialBodyEntity entity,
                                net.minecraft.client.renderer.culling.Frustum frustum,
                                double x, double y, double z) {
        return true;
    }
}
