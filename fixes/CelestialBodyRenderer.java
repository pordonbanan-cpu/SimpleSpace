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
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D-сферы планет в мировых координатах (не billboard).
 */
public class CelestialBodyRenderer extends EntityRenderer<CelestialBodyEntity> {

    private static final ResourceLocation SUN =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/sun.png");
    private static final ResourceLocation EARTH =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/earth.png");
    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/moon.png");

    private static final int LON_SEGMENTS = 32;
    private static final int LAT_SEGMENTS = 20;
    private static final float SELF_SPIN_DEG_PER_TICK = 0.04f;

    private static List<Quad> SPHERE_MESH;

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

        // Только собственное вращение планеты — без camera-facing
        float spin = (entity.tickCount + pt) * SELF_SPIN_DEG_PER_TICK;
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(radius, radius, radius);

        ResourceLocation tex = getTextureLocation(entity);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(tex));
        int fullBright = 0xF000F0;
        Matrix4f mat = pose.last().pose();
        org.joml.Matrix3f normalMat = pose.last().normal();

        for (Quad q : sphereMesh()) {
            Vector3f n = new Vector3f(q.normal);
            normalMat.transform(n);
            n.normalize();
            for (int i = 0; i < 4; i++) {
                Vector3f p = q.pos[i];
                vc.addVertex(mat, p.x, p.y, p.z)
                        .setColor(255, 255, 255, 255)
                        .setUv(q.u[i], q.v[i])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(fullBright)
                        .setNormal(n.x, n.y, n.z);
            }
        }

        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(CelestialBodyEntity entity,
                                 net.minecraft.client.renderer.culling.Frustum frustum,
                                 double x, double y, double z) {
        return true; // большие тела не отсекать frustum'ом
    }

    private record Quad(Vector3f[] pos, Vector3f normal, float[] u, float[] v) {}

    private static List<Quad> sphereMesh() {
        if (SPHERE_MESH != null) return SPHERE_MESH;

        List<Quad> quads = new ArrayList<>();
        for (int lat = 0; lat < LAT_SEGMENTS; lat++) {
            double lat0 = Math.PI * (-0.5 + (double) lat / LAT_SEGMENTS);
            double lat1 = Math.PI * (-0.5 + (double) (lat + 1) / LAT_SEGMENTS);

            for (int lon = 0; lon < LON_SEGMENTS; lon++) {
                double lon0 = 2 * Math.PI * (double) lon / LON_SEGMENTS;
                double lon1 = 2 * Math.PI * (double) (lon + 1) / LON_SEGMENTS;

                Vector3f p00 = spherePoint(lat0, lon0);
                Vector3f p01 = spherePoint(lat0, lon1);
                Vector3f p11 = spherePoint(lat1, lon1);
                Vector3f p10 = spherePoint(lat1, lon0);

                float v0 = (float) lat / LAT_SEGMENTS;
                float v1 = (float) (lat + 1) / LAT_SEGMENTS;
                float u0 = (float) lon / LON_SEGMENTS;
                float u1 = (float) (lon + 1) / LON_SEGMENTS;

                Vector3f normal = new Vector3f(p00).add(p01).add(p11).add(p10).normalize();

                quads.add(new Quad(
                        new Vector3f[]{p00, p01, p11, p10},
                        normal,
                        new float[]{u0, u1, u1, u0},
                        new float[]{v0, v0, v1, v1}
                ));
            }
        }

        SPHERE_MESH = quads;
        return quads;
    }

    private static Vector3f spherePoint(double lat, double lon) {
        double cosLat = Math.cos(lat);
        float x = (float) (cosLat * Math.cos(lon));
        float y = (float) Math.sin(lat);
        float z = (float) (cosLat * Math.sin(lon));
        return new Vector3f(x, y, z).mul(0.5f);
    }
}
