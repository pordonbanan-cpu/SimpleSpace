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
 * 3D-кубы планет в мировых координатах (не billboard).
 * Раньше тут была UV-сфера (32x20 сегментов = 640 квадов на планету),
 * теперь квадратная планета — всего 6 квадов и та же логика вращения/кэша.
 */
public class CelestialBodyRenderer extends EntityRenderer<CelestialBodyEntity> {

    private static final ResourceLocation SUN =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/sun.png");
    private static final ResourceLocation EARTH =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/earth.png");
    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/entity/moon.png");

    private static final float SELF_SPIN_DEG_PER_TICK = 0.04f;

    private static List<Quad> CUBE_MESH;

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

        for (Quad q : cubeMesh()) {
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

    /**
     * 6 faces of a unit cube (half-extent 0.5, matching the old unit-sphere
     * radius). Each face's 4 corners are wound counter-clockwise as seen
     * from outside the cube, so the outward normal matches the winding —
     * get this backwards on a face and that face renders invisible.
     */
    private static List<Quad> cubeMesh() {
        if (CUBE_MESH != null) return CUBE_MESH;

        float h = 0.5f;
        List<Quad> quads = new ArrayList<>();

        // +Y top
        quads.add(face(
                new Vector3f(-h, h, -h), new Vector3f(-h, h, h),
                new Vector3f(h, h, h), new Vector3f(h, h, -h),
                new Vector3f(0, 1, 0)));
        // -Y bottom
        quads.add(face(
                new Vector3f(-h, -h, h), new Vector3f(-h, -h, -h),
                new Vector3f(h, -h, -h), new Vector3f(h, -h, h),
                new Vector3f(0, -1, 0)));
        // +X east
        quads.add(face(
                new Vector3f(h, -h, -h), new Vector3f(h, h, -h),
                new Vector3f(h, h, h), new Vector3f(h, -h, h),
                new Vector3f(1, 0, 0)));
        // -X west
        quads.add(face(
                new Vector3f(-h, -h, -h), new Vector3f(-h, -h, h),
                new Vector3f(-h, h, h), new Vector3f(-h, h, -h),
                new Vector3f(-1, 0, 0)));
        // +Z south
        quads.add(face(
                new Vector3f(-h, -h, h), new Vector3f(h, -h, h),
                new Vector3f(h, h, h), new Vector3f(-h, h, h),
                new Vector3f(0, 0, 1)));
        // -Z north
        quads.add(face(
                new Vector3f(h, -h, -h), new Vector3f(-h, -h, -h),
                new Vector3f(-h, h, -h), new Vector3f(h, h, -h),
                new Vector3f(0, 0, -1)));

        CUBE_MESH = quads;
        return quads;
    }

    private static Quad face(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f normal) {
        // Full texture stretched over each face - simplest possible UV mapping.
        return new Quad(
                new Vector3f[]{p0, p1, p2, p3},
                normal,
                new float[]{0f, 1f, 1f, 0f},
                new float[]{1f, 1f, 0f, 0f}
        );
    }
}