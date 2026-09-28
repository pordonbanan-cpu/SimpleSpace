package com.simplespace.event;

import com.simplespace.SimpleSpace;
import com.simplespace.dimension.ModDimensions;
import com.simplespace.entity.CelestialBodyEntity;
import com.simplespace.entity.ModEntities;
import com.simplespace.space.VirtualOrbitSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Планеты-кубы стоят у «якоря» измерения; каждый тик сдвигаем их
 * относительно виртуальной орбиты игрока (Cosmonautics-style).
 */
public class SpacePlanetSpawner {

    private static boolean planetsSpawned = false;

    public static void ensurePlanets(ServerLevel spaceLevel) {
        if (planetsSpawned) return;
        spawnAll(spaceLevel);
        planetsSpawned = true;
    }

    public static void forceRespawn(ServerLevel spaceLevel) {
        for (Entity e : spaceLevel.getAllEntities()) {
            if (e instanceof CelestialBodyEntity) e.discard();
        }
        planetsSpawned = false;
        ensurePlanets(spaceLevel);
    }

    private static void spawnAll(ServerLevel level) {
        // Якорные позиции — рядом с спавном; реальная «орбита» виртуальная
        spawnPlanet(level, ModEntities.SUN.get(), 80, 40, 0);
        spawnPlanet(level, ModEntities.EARTH.get(), 0, 0, 0);
        spawnPlanet(level, ModEntities.MOON.get(), -40, 10, -20);
        SimpleSpace.LOGGER.info("Spawned cube solar system (virtual orbit)");
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() != ModDimensions.SPACE_LEVEL) return;
        if (!(event.getEntity().level() instanceof ServerLevel spaceLevel)) return;
        ensurePlanets(spaceLevel);
        // Стартовая виртуальная орбита вокруг Солнца на уровне Земли
        VirtualOrbitSystem.playerVirtualPos = new net.minecraft.world.phys.Vec3(
                VirtualOrbitSystem.ORBIT_EARTH_SUN, 0, 0);
        VirtualOrbitSystem.playerVirtualVel = new net.minecraft.world.phys.Vec3(
                0, 0, VirtualOrbitSystem.circularSpeed(
                        VirtualOrbitSystem.MU_SUN, VirtualOrbitSystem.ORBIT_EARTH_SUN));
    }

    @SubscribeEvent
    public void onSpaceTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ModDimensions.SPACE_LEVEL) return;
        if (!planetsSpawned) return;

        VirtualOrbitSystem.tick(0.05); // ~1/20 s

        var players = level.players();
        if (players.isEmpty()) return;
        var player = players.get(0);
        double t = level.getGameTime() * 0.05;
        double scale = 0.08;

        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof CelestialBodyEntity body)) continue;
            String name = body.getType().toShortString();
            String key = name.contains("sun") ? "sun"
                    : name.contains("earth") ? "earth"
                    : name.contains("moon") ? "moon" : null;
            if (key == null) continue;

            var target = VirtualOrbitSystem.renderOffset(
                    player.position(),
                    VirtualOrbitSystem.bodyVirtualPos(key, t),
                    scale
            );
            body.setPos(target.x, target.y, target.z);
            body.setDeltaMovement(0, 0, 0);
            body.setNoGravity(true);
        }
    }

    private static void spawnPlanet(ServerLevel level, EntityType<CelestialBodyEntity> type,
                                    double x, double y, double z) {
        CelestialBodyEntity planet = type.create(level);
        if (planet != null) {
            planet.moveTo(x, y, z, 0, 0);
            planet.setNoGravity(true);
            planet.setInvulnerable(true);
            level.addFreshEntity(planet);
        }
    }
}
