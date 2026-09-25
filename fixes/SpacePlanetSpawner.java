package com.simplespace.event;

import com.simplespace.SimpleSpace;
import com.simplespace.dimension.ModDimensions;
import com.simplespace.entity.CelestialBodyEntity;
import com.simplespace.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class SpacePlanetSpawner {

    private static boolean planetsSpawned = false;

    public static void ensurePlanets(ServerLevel spaceLevel) {
        if (planetsSpawned) return;
        spawnAll(spaceLevel);
        planetsSpawned = true;
    }

    public static void forceRespawn(ServerLevel spaceLevel) {
        for (Entity e : spaceLevel.getAllEntities()) {
            if (e instanceof CelestialBodyEntity) {
                e.discard();
            }
        }
        planetsSpawned = false;
        ensurePlanets(spaceLevel);
    }

    private static void spawnAll(ServerLevel level) {
        spawnPlanet(level, ModEntities.EARTH.get(), 0, 40, 0);
        spawnPlanet(level, ModEntities.MOON.get(), -180, 60, -120);
        spawnPlanet(level, ModEntities.SUN.get(), 400, 120, 500);
        SimpleSpace.LOGGER.info("Spawned celestial bodies (Earth origin, Sun far)");
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() != ModDimensions.SPACE_LEVEL) return;
        if (!(event.getEntity().level() instanceof ServerLevel spaceLevel)) return;
        ensurePlanets(spaceLevel);
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
