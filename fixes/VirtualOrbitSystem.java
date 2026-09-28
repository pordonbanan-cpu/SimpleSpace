package com.simplespace.space;

import net.minecraft.world.phys.Vec3;

/**
 * Виртуальная орбитальная система (как у Cosmonautics / Techno Build):
 *
 * — Корабль / игрок ФИЗИЧЕСКИ почти не уезжает в измерении.
 * — Реальная «орбитальная» позиция хранится отдельно (virtualPos / virtualVel).
 * — Планеты и другие корабли рисуются/смещаются относительно
 *   (их virtualPos − наш virtualPos), а не километровыми телепортами.
 *
 * Масштаб сжатый для выживания на телефоне, не реальные AU.
 */
public final class VirtualOrbitSystem {

    private VirtualOrbitSystem() {}

    /** μ сжатой «Земли» (условные единицы). */
    public static final double MU_EARTH = 400.0;
    public static final double MU_SUN = 12000.0;
    public static final double MU_MOON = 40.0;

    /** Радиусы кубов (половина стороны), блоки визуала. */
    public static final double R_SUN = 50.0;
    public static final double R_EARTH = 24.0;
    public static final double R_MOON = 7.0;

    /** Орбитальные радиусы в виртуальных метрах (сжатые). */
    public static final double ORBIT_EARTH_SUN = 800.0;
    public static final double ORBIT_MOON_EARTH = 120.0;

    /** Виртуальная позиция игрока в пространстве (обновляется с импульсами). */
    public static Vec3 playerVirtualPos = new Vec3(ORBIT_EARTH_SUN, 0, 0);
    public static Vec3 playerVirtualVel = new Vec3(0, 0, circularSpeed(MU_SUN, ORBIT_EARTH_SUN));

    public static double circularSpeed(double mu, double radius) {
        return Math.sqrt(mu / Math.max(1.0, radius));
    }

    /** Позиция тела в виртуальной системе в момент timeSec. */
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
        double w = circularSpeed(mu, radius) / radius; // rad/s
        double a = w * t + phase;
        return new Vec3(Math.cos(a) * radius, 0, Math.sin(a) * radius);
    }

    /**
     * Куда поставить entity-планету рядом с игроком, чтобы она выглядела
     * в правильном направлении и на сжатой дистанции.
     *
     * @param playerWorld  физическая позиция игрока в измерении
     * @param bodyVirtual  виртуальная позиция тела
     * @param visualScale  насколько сжать огромные расстояния для рендера (0.02–0.15)
     */
    public static Vec3 renderOffset(Vec3 playerWorld, Vec3 bodyVirtual, double visualScale) {
        Vec3 delta = bodyVirtual.subtract(playerVirtualPos).scale(visualScale);
        // Ограничиваем, чтобы entity не улетала за пределы tracking
        double max = 200.0;
        double len = delta.length();
        if (len > max) delta = delta.scale(max / len);
        return playerWorld.add(delta);
    }

    /** Применить Δv (манёвр) в виртуальной системе. */
    public static void applyImpulse(Vec3 deltaV) {
        playerVirtualVel = playerVirtualVel.add(deltaV);
    }

    /** Интеграция на dt секунд (простая гравитация к Солнцу). */
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

    public static double altitudeAboveEarth(double timeSec) {
        return playerVirtualPos.distanceTo(bodyVirtualPos("earth", timeSec)) - R_EARTH;
    }
}
