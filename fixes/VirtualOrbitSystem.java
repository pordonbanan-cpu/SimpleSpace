package com.simplespace.space;

import net.minecraft.world.phys.Vec3;

/**
 * Виртуальная орбита (Cosmonautics-style):
 * физический игрок почти на месте, движение — в virtualPos/Vel.
 */
public final class VirtualOrbitSystem {

    private VirtualOrbitSystem() {}

    public static final double MU_EARTH = 400.0;
    public static final double MU_SUN = 12000.0;
    public static final double MU_MOON = 40.0;

    /** Половина стороны куба в блоках (entity size ≈ 2*R). */
    public static final double R_SUN = 18.0;
    public static final double R_EARTH = 14.0;
    public static final double R_MOON = 5.0;

    public static final double ORBIT_EARTH_SUN = 800.0;
    public static final double ORBIT_MOON_EARTH = 120.0;
    /** Стартовая высота над Землёй (вирт.). */
    public static final double START_ALTITUDE = 90.0;

    /** Макс. дистанция entity от игрока (tracking). */
    public static final double MAX_VISUAL = 220.0;

    public static Vec3 playerVirtualPos = startPos();
    public static Vec3 playerVirtualVel = startVel();

    public static Vec3 startPos() {
        // Чуть «снаружи» орбиты Земли, чтобы куб Земли не был внутри игрока
        return new Vec3(ORBIT_EARTH_SUN + START_ALTITUDE, 20, 0);
    }

    public static Vec3 startVel() {
        return new Vec3(0, 0, circularSpeed(MU_SUN, ORBIT_EARTH_SUN));
    }

    public static void resetToEarthOrbit() {
        playerVirtualPos = startPos();
        playerVirtualVel = startVel();
    }

    public static double circularSpeed(double mu, double radius) {
        return Math.sqrt(mu / Math.max(1.0, radius));
    }

    public static Vec3 bodyVirtualPos(String body, double timeSec) {
        return switch (body) {
            case "sun" -> Vec3.ZERO;
            case "earth" -> circular(ORBIT_EARTH_SUN, MU_SUN, timeSec, 0);
            case "moon" -> {
                Vec3 earth = circular(ORBIT_EARTH_SUN, MU_SUN, timeSec, 0);
                Vec3 rel = circular(ORBIT_MOON_EARTH, MU_EARTH, timeSec, 1.7);
                yield earth.add(rel);
            }
            default -> Vec3.ZERO;
        };
    }

    private static Vec3 circular(double radius, double mu, double t, double phase) {
        double w = circularSpeed(mu, radius) / radius;
        double a = w * t + phase;
        return new Vec3(Math.cos(a) * radius, 0, Math.sin(a) * radius);
    }

    /**
     * Позиция entity у игрока.
     * Близкие тела — почти в масштабе; далёкие сжимаются, но
     * никогда не уходят дальше MAX_VISUAL (Солнце не пропадает).
     */
    public static Vec3 renderOffset(Vec3 playerWorld, Vec3 bodyVirtual, String body) {
        Vec3 delta = bodyVirtual.subtract(playerVirtualPos);
        double real = delta.length();
        if (real < 1e-4) {
            // совпали — чуть сдвинуть, чтобы не быть внутри
            return playerWorld.add(0, -R_EARTH - 2, 0);
        }

        // Нелинейный масштаб: рядом крупнее, вдали — мягкий потолок
        double scale = 0.12;
        double visual = real * scale;

        // Минимальная дистанция по типу (чтобы Солнце не залезало в лицо)
        double minVis = switch (body) {
            case "sun" -> 90.0;
            case "earth" -> 25.0;
            case "moon" -> 18.0;
            default -> 20.0;
        };
        if (visual < minVis && real > 30) visual = minVis;

        // Потолок — entity остаётся в tracking range
        if (visual > MAX_VISUAL) visual = MAX_VISUAL;

        Vec3 dir = delta.scale(1.0 / real);
        return playerWorld.add(dir.scale(visual));
    }

    public static void applyImpulse(Vec3 deltaV) {
        playerVirtualVel = playerVirtualVel.add(deltaV);
    }

    public static void tick(double dt) {
        Vec3 toSun = playerVirtualPos.scale(-1);
        double r = Math.max(1.0, toSun.length());
        Vec3 acc = toSun.normalize().scale(MU_SUN / (r * r));
        playerVirtualVel = playerVirtualVel.add(acc.scale(dt));
        playerVirtualPos = playerVirtualPos.add(playerVirtualVel.scale(dt));
    }

    public static double speed() {
        return playerVirtualVel.length();
    }

    public static double distanceToBody(String body, double timeSec) {
        return playerVirtualPos.distanceTo(bodyVirtualPos(body, timeSec));
    }
}
