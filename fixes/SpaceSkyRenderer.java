package com.simplespace.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.simplespace.dimension.ModDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Random;

@EventBusSubscriber(modid = "simplespace", value = Dist.CLIENT)
public class SpaceSkyRenderer {

    private static final ResourceLocation MILKY_WAY =
            ResourceLocation.fromNamespaceAndPath("simplespace", "textures/sky/milky_way.png");

    private static VertexBuffer starBuffer;
    private static boolean built = false;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.dimension() != ModDimensions.SPACE_LEVEL) return;

        Matrix4f pose = event.getPoseStack().last().pose();
        Matrix4f proj = event.getProjectionMatrix();

        Matrix4f modelView = new Matrix4f(pose);
        modelView.m30(0);
        modelView.m31(0);
        modelView.m32(0);

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ensureStars();
        if (starBuffer != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            starBuffer.bind();
            starBuffer.drawWithShader(modelView, proj, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
        }

        renderMilkyWay(modelView, proj);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void ensureStars() {
        if (built) return;
        built = true;

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Random rng = new Random(10842L);
        for (int i = 0; i < 3000; i++) {
            double x = rng.nextDouble() * 2 - 1;
            double y = rng.nextDouble() * 2 - 1;
            double z = rng.nextDouble() * 2 - 1;
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 0.15) continue;
            x /= len; y /= len; z /= len;

            float size = 0.12f + rng.nextFloat() * 0.4f;
            int br = 170 + rng.nextInt(86);
            int r = br, g = br, b = br;
            float tint = rng.nextFloat();
            if (tint < 0.12f) b = Math.min(255, br + 50);
            else if (tint > 0.88f) { r = Math.min(255, br + 35); g = Math.min(255, br + 15); }

            double ax = Math.abs(x) < 0.9 ? 1 : 0;
            double ay = Math.abs(x) < 0.9 ? 0 : 1;
            double px = -z * ay;
            double py = z * ax;
            double pz = x * ay - y * ax;
            double plen = Math.sqrt(px * px + py * py + pz * pz);
            if (plen < 1e-6) continue;
            px = px / plen * size;
            py = py / plen * size;
            pz = pz / plen * size;
            double qx = y * pz - z * py;
            double qy = z * px - x * pz;
            double qz = x * py - y * px;

            double dist = 100.0;
            double cx = x * dist, cy = y * dist, cz = z * dist;

            buffer.addVertex((float)(cx - px - qx), (float)(cy - py - qy), (float)(cz - pz - qz)).setColor(r, g, b, 255);
            buffer.addVertex((float)(cx + px - qx), (float)(cy + py - qy), (float)(cz + pz - qz)).setColor(r, g, b, 255);
            buffer.addVertex((float)(cx + px + qx), (float)(cy + py + qy), (float)(cz + pz + qz)).setColor(r, g, b, 255);
            buffer.addVertex((float)(cx - px + qx), (float)(cy - py + qy), (float)(cz - pz + qz)).setColor(r, g, b, 255);
        }

        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        starBuffer.bind();
        starBuffer.upload(buffer.buildOrThrow());
        VertexBuffer.unbind();
    }

    private static void renderMilkyWay(Matrix4f modelView, Matrix4f proj) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, MILKY_WAY);
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.65f);

        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int segments = 48;
        double radius = 90.0;
        double bandHalf = 20.0;
        double tilt = 28.0 * Math.PI / 180.0;

        for (int i = 0; i < segments; i++) {
            double a0 = (i / (double) segments) * Math.PI * 2;
            double a1 = ((i + 1) / (double) segments) * Math.PI * 2;
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            for (int j = 0; j < 2; j++) {
                double lat0 = (-bandHalf + j * bandHalf) * Math.PI / 180.0;
                double lat1 = (-bandHalf + (j + 1) * bandHalf) * Math.PI / 180.0;
                float v0 = j * 0.5f;
                float v1 = (j + 1) * 0.5f;

                float[] p00 = sp(a0, lat0, radius, tilt);
                float[] p10 = sp(a1, lat0, radius, tilt);
                float[] p11 = sp(a1, lat1, radius, tilt);
                float[] p01 = sp(a0, lat1, radius, tilt);

                int a = 200;
                buf.addVertex(p00[0], p00[1], p00[2]).setUv(u0, v0).setColor(190, 195, 255, a);
                buf.addVertex(p10[0], p10[1], p10[2]).setUv(u1, v0).setColor(190, 195, 255, a);
                buf.addVertex(p11[0], p11[1], p11[2]).setUv(u1, v1).setColor(190, 195, 255, a);
                buf.addVertex(p01[0], p01[1], p01[2]).setUv(u0, v1).setColor(190, 195, 255, a);
            }
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static float[] sp(double lon, double lat, double r, double tilt) {
        double x = r * Math.cos(lat) * Math.cos(lon);
        double y = r * Math.sin(lat);
        double z = r * Math.cos(lat) * Math.sin(lon);
        double y2 = y * Math.cos(tilt) - z * Math.sin(tilt);
        double z2 = y * Math.sin(tilt) + z * Math.cos(tilt);
        return new float[]{(float) x, (float) y2, (float) z2};
    }
}
