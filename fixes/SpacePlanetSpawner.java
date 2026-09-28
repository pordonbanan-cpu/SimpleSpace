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

public class SpacePlanetSpawner {

    private static boolean planetsSpawned = false;

    public static void ensurePlanets(ServerLevel spaceLevel) {
        if (planetsSpawned && countPlanets(spaceLevel) >= 3) return;
        if (planetsSpawned) {
            // кто-то despawn'нулся — пересоздать
            for (Entity e : spaceLevel.getAllEntities()) {
                if (e instanceof CelestialBodyEntity) e.discard();
            }
            planetsSpawned = false;
        }
        spawnAll(spaceLevel);
        planetsSpawned = true;
    }

    private static int countPlanets(ServerLevel level) {
        int n = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof CelestialBodyEntity) n++;
        }
        return n;
    }

    public static void forceRespawn(ServerLevel spaceLevel) {
        for (Entity e : spaceLevel.getAllEntities()) {
            if (e instanceof CelestialBodyEntity) e.discard();
        }
        planetsSpawned = false;
        ensurePlanets(spaceLevel);
        VirtualOrbitSystem.resetToEarthOrbit();
    }

    private static void spawnAll(ServerLevel level) {
        spawnPlanet(level, ModEntities.SUN.get(), 100, 40, 0);
        spawnPlanet(level, ModEntities.EARTH.get(), 0, 10, 0);
        spawnPlanet(level, ModEntities.MOON.get(), -30, 15, -25);
        SimpleSpace.LOGGER.info("Spawned cube solar system");
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() != ModDimensions.SPACE_LEVEL) return;
        if (!(event.getEntity().level() instanceof ServerLevel spaceLevel)) return;
        ensurePlanets(spaceLevel);
        VirtualOrbitSystem.resetToEarthOrbit();
    }

    @SubscribeEvent
    public void onSpaceTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ModDimensions.SPACE_LEVEL) return;

        ensurePlanets(level);
        VirtualOrbitSystem.tick(0.05);

        var players = level.players();
        if (players.isEmpty()) return;
        var player = players.get(0);
        double t = level.getGameTime() * 0.05;

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
                    key
            );
            body.setPos(target.x, target.y, target.z);
            body.setDeltaMovement(0, 0, 0);
            body.setNoGravity(true);
            body.setInvisible(false);
        }
    }

    private static void spawnPlanet(ServerLevel level, EntityType<CelestialBodyEntity> type,
                                    double x, double y, double z) {
        CelestialBodyEntity planet = type.create(level);
        if (planet != null) {
            planet.moveTo(x, y, z, 0, 0);
            planet.setNoGravity(true);
            planet.setInvulnerable(true);
            planet.setSilent(true);
            level.addFreshEntity(planet);
        }
    }
}
